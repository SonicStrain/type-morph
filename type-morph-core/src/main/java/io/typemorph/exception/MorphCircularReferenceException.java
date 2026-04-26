package io.typemorph.exception;

/**
 * Thrown when a circular reference is detected during reflective mapping.
 * For example, ClassA has a field of type ClassA that is also being mapped.
 *
 * <p>Detected via a thread-local identity map tracking objects currently in-flight.
 */
public class MorphCircularReferenceException extends MorphException {

    private final Class<?> cycleType;

    public MorphCircularReferenceException(Class<?> cycleType) {
        super("Circular reference detected: object of type " + cycleType.getName()
              + " is already being mapped in the current call stack. "
              + "Use @TypeMorphIgnore to exclude the circular field, "
              + "or restructure your classes to break the cycle.");
        this.cycleType = cycleType;
    }

    public Class<?> getCycleType() { return cycleType; }
}
