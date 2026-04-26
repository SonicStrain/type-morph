package io.typemorph.exception;

/**
 * Thrown when a field's source type cannot be converted to the target field type —
 * either because no built-in conversion exists and deepMap is false, or because
 * a conversion was attempted and failed.
 */
public class MorphFieldTypeException extends MorphException {

    private final Class<?> sourceFieldType;
    private final Class<?> targetFieldType;

    public MorphFieldTypeException(Class<?> sourceFieldType, Class<?> targetFieldType) {
        super("Cannot convert field type " + sourceFieldType.getName()
              + " to " + targetFieldType.getName()
              + ". Register a TypeMorph mapping between these types, "
              + "or use @TypeMorphField(deepMap = true) to enable nested mapping.");
        this.sourceFieldType = sourceFieldType;
        this.targetFieldType = targetFieldType;
    }

    public MorphFieldTypeException(Class<?> sourceFieldType, Class<?> targetFieldType, Throwable cause) {
        super("Cannot convert field type " + sourceFieldType.getName()
              + " to " + targetFieldType.getName() + ": " + cause.getMessage(), cause);
        this.sourceFieldType = sourceFieldType;
        this.targetFieldType = targetFieldType;
    }

    public Class<?> getSourceFieldType() { return sourceFieldType; }
    public Class<?> getTargetFieldType() { return targetFieldType; }
}
