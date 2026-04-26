package io.typemorph.annotation;

import java.lang.annotation.*;

/**
 * Container for repeatable {@link TypeMorphIgnore} annotations.
 * Use repeated {@code @TypeMorphIgnore} directly — this container is used automatically.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TypeMorphIgnores {
    TypeMorphIgnore[] value();
}
