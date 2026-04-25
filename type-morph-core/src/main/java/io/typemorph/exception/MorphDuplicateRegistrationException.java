package io.typemorph.exception;

public class MorphDuplicateRegistrationException extends MorphException {

    private final Class<?> sourceType;
    private final Class<?> targetType;

    public MorphDuplicateRegistrationException(Class<?> sourceType, Class<?> targetType) {
        super("A MorphMapping for [" + sourceType.getName() + " -> " + targetType.getName()
              + "] is already registered. Duplicate registrations are not allowed.");
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
