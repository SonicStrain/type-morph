package io.typemorph.exception;

/**
 * Thrown when a target class cannot be instantiated — e.g., no accessible
 * no-arg constructor, abstract class, interface, or constructor invocation failure.
 */
public class MorphInstantiationException extends MorphException {

    private final Class<?> targetType;

    public MorphInstantiationException(Class<?> targetType, String reason) {
        super("Cannot instantiate target type " + targetType.getName() + ": " + reason
              + ". Ensure the class has a public no-arg constructor, "
              + "is a record, or annotate a constructor with @TypeMorphConstructor.");
        this.targetType = targetType;
    }

    public MorphInstantiationException(Class<?> targetType, String reason, Throwable cause) {
        super("Cannot instantiate target type " + targetType.getName() + ": " + reason, cause);
        this.targetType = targetType;
    }

    public Class<?> getTargetType() { return targetType; }
}
