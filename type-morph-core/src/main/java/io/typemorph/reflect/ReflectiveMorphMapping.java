package io.typemorph.reflect;

import io.typemorph.TypeMorph;
import io.typemorph.config.NullFieldBehavior;
import io.typemorph.exception.MorphFieldMappingException;
import io.typemorph.mapping.MorphMapping;

import java.lang.reflect.Field;

/**
 * A {@link MorphMapping} implementation that transfers field values from a source
 * object to a newly created target object using pre-built {@link FieldBinding}s.
 *
 * <p>All reflection work (finding fields, setting accessible) is done at build time
 * by {@link ReflectiveMappingBuilder}. At mapping time this class only reads and
 * writes field values — no annotation scanning, no type resolution.
 *
 * <h3>Record targets</h3>
 * Records have final backing fields and must be constructed in one shot via their
 * canonical constructor. This class collects all mapped values into an {@code Object[]}
 * indexed by record component position, then calls the canonical constructor once.
 * Unmapped positions stay {@code null}.
 *
 * <h3>POJO targets</h3>
 * A new instance is created first (via {@link InstantiationStrategy}), then each
 * target field is set individually.
 *
 * <h3>Cycle detection</h3>
 * Each mapping call registers the source object with {@link CycleGuard}. If the same
 * object (by identity) is already being mapped in the current thread's call stack, a
 * {@link io.typemorph.exception.MorphCircularReferenceException} is thrown.
 *
 * @param <S> source type
 * @param <T> target type
 */
public class ReflectiveMorphMapping<S, T> implements MorphMapping<S, T> {

    private final Class<S>                 sourceClass;
    private final Class<T>                 targetClass;
    private final FieldBinding[]           bindings;
    private final InstantiationStrategy<T> strategy;
    private final TypeMorph                typeMorph;   // null when deepMap not needed

    ReflectiveMorphMapping(Class<S>                 sourceClass,
                           Class<T>                 targetClass,
                           FieldBinding[]           bindings,
                           InstantiationStrategy<T> strategy,
                           TypeMorph                typeMorph) {
        this.sourceClass = sourceClass;
        this.targetClass = targetClass;
        this.bindings    = bindings;
        this.strategy    = strategy;
        this.typeMorph   = typeMorph;
    }

    // -------------------------------------------------------------------------
    // MorphMapping implementation
    // -------------------------------------------------------------------------

    @Override
    public T map(S source) {
        if (source == null) return null;

        CycleGuard.INSTANCE.beginMapping(source);
        try {
            return targetClass.isRecord()
                    ? mapToRecord(source)
                    : mapToPojo(source);
        } finally {
            CycleGuard.INSTANCE.endMapping(source);
        }
    }

    // -------------------------------------------------------------------------
    // Record path
    // -------------------------------------------------------------------------

    private T mapToRecord(S source) {
        // Count record components (strategy knows, but derive from first record binding)
        int componentCount = targetClass.getRecordComponents().length;
        Object[] args = new Object[componentCount];

        for (FieldBinding b : bindings) {
            Object value = readSourceField(b, source);
            if (value == null) {
                if (!applyNullBehaviorRecord(b, args)) continue; // SKIP or already set
            } else {
                value = convertValue(b, value);
                args[b.targetComponentIndex()] = value;
            }
        }

        return strategy.newInstance(args);
    }

    /**
     * Applies null behavior for record targets. Returns false if the binding should
     * be skipped (SKIP), true if a null has been placed (SET_NULL), throws for THROW.
     */
    private boolean applyNullBehaviorRecord(FieldBinding b, Object[] args) {
        return switch (b.onNull()) {
            case SKIP     -> false;
            case SET_NULL -> {
                args[b.targetComponentIndex()] = null;
                yield true;
            }
            case THROW    -> throw new MorphFieldMappingException(
                    sourceClass, targetClass, b.sourceFieldName(),
                    "source field value is null and NullFieldBehavior.THROW is configured");
        };
    }

    // -------------------------------------------------------------------------
    // POJO path
    // -------------------------------------------------------------------------

    private T mapToPojo(S source) {
        T target = strategy.newInstance(new Object[0]);

        for (FieldBinding b : bindings) {
            Object value = readSourceField(b, source);
            if (value == null) {
                applyNullBehaviorPojo(b, target);
            } else {
                value = convertValue(b, value);
                writeTargetField(b, target, value);
            }
        }

        return target;
    }

    private void applyNullBehaviorPojo(FieldBinding b, T target) {
        switch (b.onNull()) {
            case SKIP     -> { /* do nothing — leave target field at its initialized value */ }
            case SET_NULL -> writeTargetField(b, target, null);
            case THROW    -> throw new MorphFieldMappingException(
                    sourceClass, targetClass, b.sourceFieldName(),
                    "source field value is null and NullFieldBehavior.THROW is configured");
        }
    }

    // -------------------------------------------------------------------------
    // Value conversion
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Object convertValue(FieldBinding b, Object value) {
        if (b.deepMap()) {
            if (typeMorph == null) {
                throw new MorphFieldMappingException(sourceClass, targetClass, b.sourceFieldName(),
                        "deepMap=true but no TypeMorph instance is available for nested mapping. "
                        + "Ensure this mapping was registered via TypeMorph.scan().");
            }
            return typeMorph.map(value);
        }
        if (b.converter() != null) {
            return b.converter().convert(value, b.targetFieldType());
        }
        return value;
    }

    // -------------------------------------------------------------------------
    // Reflection helpers
    // -------------------------------------------------------------------------

    private Object readSourceField(FieldBinding b, S source) {
        Field f = b.sourceField();
        try {
            return f.get(source);
        } catch (IllegalAccessException e) {
            throw new MorphFieldMappingException(sourceClass, targetClass, f.getName(),
                    "cannot read source field (setAccessible failed)", e);
        }
    }

    private void writeTargetField(FieldBinding b, T target, Object value) {
        Field f = b.targetField();
        if (f == null) {
            // Defensive: should not happen for POJO (records are handled separately)
            throw new MorphFieldMappingException(sourceClass, targetClass, b.sourceFieldName(),
                    "target field is null for a POJO binding — this is a bug in ReflectiveMappingBuilder");
        }
        try {
            f.set(target, value);
        } catch (IllegalAccessException e) {
            throw new MorphFieldMappingException(sourceClass, targetClass, f.getName(),
                    "cannot write target field (setAccessible failed)", e);
        } catch (IllegalArgumentException e) {
            throw new MorphFieldMappingException(sourceClass, targetClass, f.getName(),
                    "type mismatch when writing target field: expected " + f.getType().getName()
                    + " but value is " + (value == null ? "null" : value.getClass().getName()), e);
        }
    }

    // -------------------------------------------------------------------------
    // Accessors (useful for diagnostics)
    // -------------------------------------------------------------------------

    public Class<S> getSourceClass()    { return sourceClass; }
    public Class<T> getTargetClass()    { return targetClass; }
    public int      getBindingCount()   { return bindings.length; }
}
