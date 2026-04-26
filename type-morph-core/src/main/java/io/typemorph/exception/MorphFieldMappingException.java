package io.typemorph.exception;

/**
 * Thrown when a field-level mapping operation fails — e.g., null source value
 * for a primitive target field, or inaccessible field.
 */
public class MorphFieldMappingException extends MorphException {

    private final Class<?> sourceType;
    private final Class<?> targetType;
    private final String fieldName;

    public MorphFieldMappingException(Class<?> sourceType, Class<?> targetType,
                                      String fieldName, String reason) {
        super("Field mapping failed: " + sourceType.getName() + " -> " + targetType.getName()
              + ", field [" + fieldName + "]: " + reason);
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.fieldName  = fieldName;
    }

    public MorphFieldMappingException(Class<?> sourceType, Class<?> targetType,
                                      String fieldName, String reason, Throwable cause) {
        super("Field mapping failed: " + sourceType.getName() + " -> " + targetType.getName()
              + ", field [" + fieldName + "]: " + reason, cause);
        this.sourceType = sourceType;
        this.targetType = targetType;
        this.fieldName  = fieldName;
    }

    public Class<?> getSourceType() { return sourceType; }
    public Class<?> getTargetType() { return targetType; }
    public String   getFieldName()  { return fieldName;  }
}
