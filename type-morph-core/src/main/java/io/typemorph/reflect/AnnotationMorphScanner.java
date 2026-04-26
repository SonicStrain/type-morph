package io.typemorph.reflect;

import io.typemorph.TypeMorph;
import io.typemorph.annotation.TypeMorphClass;
import io.typemorph.exception.MorphConfigurationException;

/**
 * Scans classes annotated with {@link TypeMorphClass} and registers reflective
 * {@link ReflectiveMorphMapping}s into a {@link TypeMorph} instance.
 *
 * <p>Called internally by {@link TypeMorph#scan(Class[])}. Not normally used directly.
 *
 * <h3>Registration order for bidirectional mappings</h3>
 * When {@code @TypeMorphClass(bidirectional = true)}, two mappings are registered:
 * <ol>
 *   <li>source → target (built from the source class annotations)</li>
 *   <li>target → source (built from the target class — no annotations needed on target)</li>
 * </ol>
 * The reverse mapping uses the same field name resolution rules: fields are matched
 * by name; source annotations on the original source class are NOT applied in reverse.
 */
public class AnnotationMorphScanner {

    private final TypeMorph typeMorph;

    public AnnotationMorphScanner(TypeMorph typeMorph) {
        this.typeMorph = typeMorph;
    }

    /**
     * Processes the given classes, registering a mapping for each
     * {@code @TypeMorphClass} annotation found.
     *
     * @param classes one or more classes to scan; non-annotated classes are silently skipped
     * @throws MorphConfigurationException if annotation configuration is invalid
     */
    public void scan(Class<?>... classes) {
        for (Class<?> clazz : classes) {
            TypeMorphClass ann = clazz.getDeclaredAnnotation(TypeMorphClass.class);
            if (ann == null) continue; // silently skip unannotated classes

            Class<?>[] targets = ann.targets();
            if (targets == null || targets.length == 0) {
                throw new MorphConfigurationException(
                        "@TypeMorphClass on " + clazz.getName()
                        + " must declare at least one target class.");
            }

            for (Class<?> target : targets) {
                registerForward(clazz, target);
                if (ann.bidirectional()) {
                    registerReverse(clazz, target);
                }
            }
        }
    }

    // -------------------------------------------------------------------------

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerForward(Class<?> source, Class<?> target) {
        ReflectiveMorphMapping<?, ?> mapping =
                new ReflectiveMappingBuilder(source, target, typeMorph).build();
        typeMorph.register((Class) source, (Class) target, (ReflectiveMorphMapping) mapping);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerReverse(Class<?> source, Class<?> target) {
        // Reverse: target → source.  The target class has no @TypeMorphClass annotation;
        // the builder falls back to name-based field matching for all fields.
        ReflectiveMorphMapping<?, ?> reverseMapping =
                new ReflectiveMappingBuilder(target, source, typeMorph).build();
        typeMorph.register((Class) target, (Class) source, (ReflectiveMorphMapping) reverseMapping);
    }
}
