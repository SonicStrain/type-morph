package io.typemorph.exception;

public class MorphConversionException extends MorphException {

    private final Class<?> sourceType;
    private final Class<?> targetType;

    public MorphConversionException(Class<?> sourceType, Class<?> targetType, Throwable cause) {
        super("Mapping from " + sourceType.getName() + " to " + targetType.getName()
              + " threw an exception: " + cause.getMessage(), cause);
        this.sourceType = sourceType;
        this.targetType = targetType;
    }

    public Class<?> getSourceType() {
        return sourceType;
    }

    public Class<?> getTargetType() {
        return targetType;
    }
}
