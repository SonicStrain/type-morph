package io.typemorph.annotation;

import java.lang.annotation.*;

/**
 * Optional marker annotation on MorphMapping implementations.
 * Explicitly declares source and target types, helping when generic type
 * resolution from the class hierarchy is not possible (e.g., raw-typed registrations).
 *
 * <pre>
 *   {@literal @}MorphMapper(source = ClassB.class, target = ClassA.class)
 *   public class ClassBMapper implements MorphMapping{@literal <}ClassB, ClassA{@literal >} { ... }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface MorphMapper {
    Class<?> source() default Void.class;
    Class<?> target() default Void.class;
}
