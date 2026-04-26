package io.typemorph.reflect;

import io.typemorph.TypeMorph;
import io.typemorph.annotation.TypeMorphConstructor;
import io.typemorph.annotation.TypeMorphField;
import io.typemorph.annotation.TypeMorphIgnore;
import io.typemorph.config.NullFieldBehavior;
import io.typemorph.exception.MorphConfigurationException;

import java.lang.reflect.*;
import java.util.*;

/**
 * Builds a {@link ReflectiveMorphMapping} from reflection metadata and annotations.
 *
 * <p>All validation and field resolution happens at build time (startup), not at
 * mapping time. This means configuration errors surface immediately, not during
 * the first mapping call.
 *
 * <p>Build order:
 * <ol>
 *   <li>Collect all non-static fields from the source class hierarchy</li>
 *   <li>For each source field, resolve the target field (by annotation or name)</li>
 *   <li>Validate: no duplicate {@code @TypeMorphField} for the same target,
 *       no {@code SET_NULL} on primitive targets, no final POJO target fields</li>
 *   <li>Call {@code setAccessible(true)} once per field</li>
 *   <li>Determine the target instantiation strategy (record / no-arg / @TypeMorphConstructor)</li>
 *   <li>Build the {@link FieldBinding} array</li>
 * </ol>
 */
public class ReflectiveMappingBuilder<S, T> {

    private final Class<S>     sourceClass;
    private final Class<T>     targetClass;
    private final TypeMorph    typeMorph;
    private final TypeConverter converter = new DefaultTypeConverter();

    public ReflectiveMappingBuilder(Class<S> sourceClass, Class<T> targetClass, TypeMorph typeMorph) {
        this.sourceClass = Objects.requireNonNull(sourceClass, "sourceClass");
        this.targetClass = Objects.requireNonNull(targetClass, "targetClass");
        this.typeMorph   = typeMorph; // may be null for builds without deepMap support
    }

    /**
     * Builds and returns the {@link ReflectiveMorphMapping}.
     * Throws {@link MorphConfigurationException} for any configuration error.
     */
    public ReflectiveMorphMapping<S, T> build() {
        validateTargetInstantiable();

        List<Field> sourceFields  = collectAllFields(sourceClass);
        Map<String, Field> targetFieldMap = buildTargetFieldMap();

        List<FieldBinding> bindings = new ArrayList<>();

        // Track which @TypeMorphField annotations have been seen per source-field×target pair
        // to detect duplicates
        Map<String, Set<Class<?>>> seenAnnotations = new HashMap<>();

        for (Field sourceField : sourceFields) {
            checkDuplicateAnnotations(sourceField, seenAnnotations);

            if (isIgnored(sourceField, targetClass)) continue;

            Optional<FieldMeta> meta = resolveFieldMeta(sourceField, targetClass);
            if (meta.isEmpty()) continue;

            FieldMeta fm = meta.get();
            String targetFieldName = fm.targetName();

            // Resolve target component index for records
            int componentIndex = targetClass.isRecord()
                    ? resolveRecordComponentIndex(targetFieldName)
                    : -1;

            // Resolve target field object (null for record targets where we use constructor args)
            Field targetField = targetFieldMap.get(targetFieldName);

            if (targetField == null && componentIndex < 0) {
                // Field name not found in target and not a record component — silently skip
                continue;
            }

            // For non-record targets: validate and prepare the target field
            Class<?> targetFieldType;
            if (!targetClass.isRecord()) {
                if (targetField == null) continue; // already handled above, but be safe
                validateTargetField(targetField);
                makeAccessible(targetField, "target field");
                targetFieldType = targetField.getType();
            } else {
                // For records, get type from the record component
                targetFieldType = getRecordComponentType(targetFieldName);
                if (targetFieldType == null) continue; // component not found — skip
            }

            // Validate SET_NULL on primitive target
            if (fm.onNull() == NullFieldBehavior.SET_NULL && targetFieldType.isPrimitive()) {
                throw new MorphConfigurationException(
                        "NullFieldBehavior.SET_NULL cannot be used on primitive field ["
                        + targetFieldName + "] in " + targetClass.getName()
                        + " (primitives cannot be null). "
                        + "Use NullFieldBehavior.SKIP or THROW instead.");
            }

            // Grant access to source field
            makeAccessible(sourceField, "source field");

            // Determine converter (only needed when types differ)
            TypeConverter fieldConverter = null;
            if (!fm.deepMap()) {
                Class<?> sourceFieldType = sourceField.getType();
                if (!isCompatible(sourceFieldType, targetFieldType)) {
                    if (!converter.canConvert(sourceFieldType, targetFieldType)) {
                        throw new MorphConfigurationException(
                                "Field [" + sourceField.getName() + "] in " + sourceClass.getName()
                                + " has type " + sourceFieldType.getName()
                                + " which cannot be converted to " + targetFieldType.getName()
                                + " in " + targetClass.getName()
                                + ". Use @TypeMorphField(deepMap = true) if a TypeMorph mapping exists, "
                                + "or register a custom TypeConverter.");
                    }
                    fieldConverter = converter;
                }
            }

            bindings.add(new FieldBinding(
                    sourceField,
                    targetField,        // null for records
                    componentIndex,     // -1 for POJOs
                    fm.deepMap(),
                    fm.onNull(),
                    fieldConverter
            ));
        }

        InstantiationStrategy<T> strategy = buildInstantiationStrategy();
        return new ReflectiveMorphMapping<>(
                sourceClass, targetClass, bindings.toArray(FieldBinding[]::new), strategy, typeMorph);
    }

    // -------------------------------------------------------------------------
    // Field collection
    // -------------------------------------------------------------------------

    /**
     * Collects all non-static declared fields from the class and its superclasses,
     * stopping at (but not including) Object.
     */
    private List<Field> collectAllFields(Class<?> clazz) {
        List<Field> fields = new ArrayList<>();
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            for (Field f : current.getDeclaredFields()) {
                if (!Modifier.isStatic(f.getModifiers())) {
                    fields.add(f);
                }
            }
            current = current.getSuperclass();
        }
        return fields;
    }

    /** Builds a name→Field map for all non-static fields in the target class hierarchy. */
    private Map<String, Field> buildTargetFieldMap() {
        Map<String, Field> map = new LinkedHashMap<>();
        for (Field f : collectAllFields(targetClass)) {
            map.putIfAbsent(f.getName(), f); // subclass fields take precedence
        }
        return map;
    }

    // -------------------------------------------------------------------------
    // Annotation resolution
    // -------------------------------------------------------------------------

    /** Returns true if the source field should be ignored for the given target class. */
    private boolean isIgnored(Field sourceField, Class<?> target) {
        for (TypeMorphIgnore ann : sourceField.getDeclaredAnnotationsByType(TypeMorphIgnore.class)) {
            if (ann.target() == Void.class || ann.target() == target) return true;
        }
        return false;
    }

    /** Returns the field mapping metadata for a source field / target pair, or empty if skipped. */
    private Optional<FieldMeta> resolveFieldMeta(Field sourceField, Class<?> target) {
        TypeMorphField[] annotations = sourceField.getDeclaredAnnotationsByType(TypeMorphField.class);

        // Find annotation that matches this target (specific match preferred over wildcard)
        TypeMorphField specific  = null;
        TypeMorphField wildcard  = null;

        for (TypeMorphField ann : annotations) {
            if (ann.target() == target) {
                if (specific != null) {
                    throw new MorphConfigurationException(
                            "Duplicate @TypeMorphField for target " + target.getName()
                            + " on field [" + sourceField.getName() + "] in "
                            + sourceField.getDeclaringClass().getName()
                            + ". Only one @TypeMorphField per target is allowed.");
                }
                specific = ann;
            } else if (ann.target() == Void.class) {
                if (wildcard != null) {
                    throw new MorphConfigurationException(
                            "Duplicate @TypeMorphField(target = Void.class) on field ["
                            + sourceField.getName() + "] in "
                            + sourceField.getDeclaringClass().getName()
                            + ". Use target = SpecificClass.class to disambiguate.");
                }
                wildcard = ann;
            }
        }

        TypeMorphField chosen = specific != null ? specific : wildcard;

        if (chosen != null) {
            String name = chosen.name().isEmpty() ? sourceField.getName() : chosen.name();
            return Optional.of(new FieldMeta(name, chosen.deepMap(), chosen.onNull()));
        }

        // No annotation — default: same field name, SKIP null, no deepMap
        return Optional.of(new FieldMeta(sourceField.getName(), false, NullFieldBehavior.SKIP));
    }

    /** Detects duplicate @TypeMorphField annotations for the same specific target. */
    private void checkDuplicateAnnotations(Field field,
                                           Map<String, Set<Class<?>>> seen) {
        String key = field.getDeclaringClass().getName() + "#" + field.getName();
        seen.putIfAbsent(key, new HashSet<>());
        // Actual duplicate detection is done inside resolveFieldMeta per invocation
    }

    // -------------------------------------------------------------------------
    // Target validation
    // -------------------------------------------------------------------------

    private void validateTargetInstantiable() {
        if (targetClass.isRecord()) return; // records always have a canonical constructor
        if (targetClass.isInterface())
            throw new MorphConfigurationException("Cannot map to interface type: " + targetClass.getName());
        if (Modifier.isAbstract(targetClass.getModifiers()))
            throw new MorphConfigurationException("Cannot map to abstract class: " + targetClass.getName());
    }

    private void validateTargetField(Field targetField) {
        if (Modifier.isFinal(targetField.getModifiers())) {
            throw new MorphConfigurationException(
                    "Cannot map to final field [" + targetField.getName()
                    + "] in " + targetClass.getName()
                    + ". Make the field non-final, use a Java record, "
                    + "or annotate a constructor with @TypeMorphConstructor.");
        }
    }

    // -------------------------------------------------------------------------
    // Record component helpers
    // -------------------------------------------------------------------------

    private int resolveRecordComponentIndex(String componentName) {
        RecordComponent[] components = targetClass.getRecordComponents();
        for (int i = 0; i < components.length; i++) {
            if (components[i].getName().equals(componentName)) return i;
        }
        return -1; // component not found
    }

    private Class<?> getRecordComponentType(String componentName) {
        for (RecordComponent rc : targetClass.getRecordComponents()) {
            if (rc.getName().equals(componentName)) return rc.getType();
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Instantiation strategy
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private InstantiationStrategy<T> buildInstantiationStrategy() {
        if (targetClass.isRecord()) {
            // Canonical constructor: parameter types match record component types in order
            RecordComponent[] components = targetClass.getRecordComponents();
            Class<?>[] paramTypes = Arrays.stream(components)
                    .map(RecordComponent::getType)
                    .toArray(Class[]::new);
            try {
                Constructor<T> canonical = targetClass.getDeclaredConstructor(paramTypes);
                canonical.setAccessible(true);
                return new RecordInstantiationStrategy<>(canonical, components.length);
            } catch (NoSuchMethodException e) {
                throw new MorphConfigurationException(
                        "Could not find canonical constructor for record " + targetClass.getName(), e);
            }
        }

        // Try @TypeMorphConstructor first
        for (Constructor<?> ctor : targetClass.getDeclaredConstructors()) {
            if (ctor.isAnnotationPresent(TypeMorphConstructor.class)) {
                ctor.setAccessible(true);
                return new AnnotatedConstructorInstantiationStrategy<>((Constructor<T>) ctor);
            }
        }

        // Try public no-arg constructor
        try {
            Constructor<T> noArg = targetClass.getDeclaredConstructor();
            noArg.setAccessible(true);
            return new NoArgInstantiationStrategy<>(noArg);
        } catch (NoSuchMethodException e) {
            throw new MorphConfigurationException(
                    "No accessible constructor found for target class " + targetClass.getName()
                    + ". Add a public no-arg constructor, annotate a constructor with "
                    + "@TypeMorphConstructor, or use a Java record.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Accessibility helper
    // -------------------------------------------------------------------------

    private void makeAccessible(Field field, String role) {
        try {
            field.setAccessible(true);
        } catch (Exception e) {
            throw new MorphConfigurationException(
                    "Cannot access " + role + " [" + field.getName() + "] in "
                    + field.getDeclaringClass().getName()
                    + ". Add --add-opens to your JVM args or provide a public getter/setter.", e);
        }
    }

    // -------------------------------------------------------------------------
    // Type compatibility
    // -------------------------------------------------------------------------

    private boolean isCompatible(Class<?> source, Class<?> target) {
        if (target.isAssignableFrom(source)) return true;
        // primitive ↔ wrapper compatibility
        if (source.isPrimitive() || target.isPrimitive()) {
            Class<?> w1 = wrap(source);
            Class<?> w2 = wrap(target);
            return w1 == w2;
        }
        return false;
    }

    private static final Map<Class<?>, Class<?>> PRIMITIVES = Map.of(
            int.class, Integer.class, long.class, Long.class, double.class, Double.class,
            float.class, Float.class, boolean.class, Boolean.class, short.class, Short.class,
            byte.class, Byte.class, char.class, Character.class
    );

    private Class<?> wrap(Class<?> c) {
        return c.isPrimitive() ? PRIMITIVES.getOrDefault(c, c) : c;
    }

    // -------------------------------------------------------------------------
    // Internal value types
    // -------------------------------------------------------------------------

    /** Resolved mapping metadata for one source field → target field. */
    private record FieldMeta(String targetName, boolean deepMap, NullFieldBehavior onNull) {}
}
