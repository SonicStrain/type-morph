package io.typemorph.annotation;

import java.lang.annotation.*;

/**
 * Container annotation for multiple {@link TypeMorphField} on the same field.
 * You do not need to use this directly — use repeated {@code @TypeMorphField}.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TypeMorphFields {
    TypeMorphField[] value();
}
