package io.typemorph.annotation;

import java.lang.annotation.*;

/**
 * Marks a class as a reflective type-morph source, declaring one or more target types
 * it can be automatically mapped to via field reflection.
 *
 * <p>The library will inspect all non-static fields of the annotated class and attempt
 * to map them to the target class by matching field names. Fields with different names
 * can be mapped using {@link TypeMorphField}.
 *
 * <pre>
 * {@literal @}TypeMorphClass(targets = ClassB.class)
 * public class ClassC {
 *
 *     {@literal @}TypeMorphField(name = "bField")  // cField in ClassC → bField in ClassB
 *     private String cField;
 *
 *     private String sameNameField;   // auto-mapped (same name exists in ClassB)
 * }
 *
 * // Register and use:
 * morph.scan(ClassC.class);
 * ClassB b = morph.map(classC, ClassB.class);
 * // Or — same method, different pair:
 * ClassB b = morph.map(classC);
 * </pre>
 *
 * <p>After annotating your class, call {@link io.typemorph.TypeMorph#scan(Class[])}
 * to register the generated mapping.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TypeMorphClass {

    /**
     * The target class(es) this source class can be mapped to.
     * At least one target is required.
     */
    Class<?>[] targets();

    /**
     * When true, the library also registers the reverse mapping (target → source)
     * by inverting the field bindings built from this annotation.
     * The reverse mapping does not require any annotations on the target class.
     */
    boolean bidirectional() default false;
}
