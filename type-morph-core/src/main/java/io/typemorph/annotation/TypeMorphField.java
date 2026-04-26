package io.typemorph.annotation;

import io.typemorph.config.NullFieldBehavior;

import java.lang.annotation.*;

/**
 * Placed on a field of a {@link TypeMorphClass}-annotated source class to customize
 * how that field is mapped to a target.
 *
 * <p>This annotation is repeatable — use multiple to specify different mappings
 * for different target classes.
 *
 * <h2>Same name, different target</h2>
 * <pre>
 *   {@literal @}TypeMorphField(target = ClassB.class, name = "bField")
 *   {@literal @}TypeMorphField(target = ClassC.class, name = "cField")
 *   private String myField;
 * </pre>
 *
 * <h2>Deep map — nested object mapping via TypeMorph</h2>
 * <pre>
 *   {@literal @}TypeMorphField(deepMap = true)
 *   private SubEntity subEntity;  // SubEntity must have a registered mapping to SubDto
 * </pre>
 *
 * <h2>Null handling per field</h2>
 * <pre>
 *   {@literal @}TypeMorphField(onNull = NullFieldBehavior.THROW)
 *   private String required;
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Repeatable(TypeMorphFields.class)
@Documented
public @interface TypeMorphField {

    /**
     * The target class this annotation applies to.
     * {@code Void.class} (default) means "apply to ALL targets declared in @TypeMorphClass".
     */
    Class<?> target() default Void.class;

    /**
     * The name of the corresponding field in the target class.
     * Empty string (default) means "use the same field name as the source".
     */
    String name() default "";

    /**
     * When true, the field value is not directly assigned to the target field.
     * Instead, {@code TypeMorph.map(sourceValue, targetFieldType)} is called,
     * enabling nested/deep object mapping.
     *
     * <p>Requires a TypeMorph mapping to be registered between the source field
     * type and the target field type.
     */
    boolean deepMap() default false;

    /**
     * Controls behavior when this field's value is null in the source object.
     * Defaults to {@link NullFieldBehavior#SKIP} — the target field is left
     * at its JVM default.
     */
    NullFieldBehavior onNull() default NullFieldBehavior.SKIP;
}
