package io.typemorph.exception;

public class MorphNotFoundException extends MorphException {

    private final Class<?> sourceType;

    public MorphNotFoundException(Class<?> sourceType) {
        super("No MorphMapping registered for source type: " + sourceType.getName()
              + ". Register a mapping via TypeMorph.register().");
        this.sourceType = sourceType;
    }

    public MorphNotFoundException(Class<?> sourceType, Class<?> targetType) {
        super("No MorphMapping registered for [" + sourceType.getName()
              + " -> " + targetType.getName() + "]. Register a mapping via TypeMorph.register().");
        this.sourceType = sourceType;
    }

    public Class<?> getSourceType() {
        return sourceType;
    }
}
