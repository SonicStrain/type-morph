package io.typemorph.annotation;

import java.lang.annotation.*;

/**
 * Designates a constructor in the target class to be used for instantiation
 * during reflective mapping, when no public no-arg constructor is available.
 *
 * <p>The designated constructor's parameters are matched to target fields
 * by parameter name (requires compilation with {@code -parameters} flag,
 * or by position as a fallback).
 *
 * <pre>
 * public class ClassB {
 *
 *     private final String name;
 *     private final int age;
 *
 *     {@literal @}TypeMorphConstructor
 *     public ClassB(String name, int age) {
 *         this.name = name;
 *         this.age  = age;
 *     }
 * }
 * </pre>
 *
 * <p>Note: Java records automatically use their canonical constructor and
 * do not need this annotation.
 */
@Target(ElementType.CONSTRUCTOR)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TypeMorphConstructor {
}
