package io.typemorph.annotation;

import java.lang.annotation.*;

/**
 * Placed on a field of a {@link TypeMorphClass}-annotated class to exclude it
 * from the mapping to one or all target types.
 *
 * <pre>
 * // Ignore for ALL targets:
 * {@literal @}TypeMorphIgnore
 * private String internalField;
 *
 * // Ignore for a specific target only:
 * {@literal @}TypeMorphIgnore(target = ClassB.class)
 * private String partiallyIgnoredField;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(TypeMorphIgnores.class)
@Documented
public @interface TypeMorphIgnore {

    /**
     * The target class to ignore this field for.
     * {@code Void.class} (default) means "ignore for ALL targets".
     */
    Class<?> target() default Void.class;
}
