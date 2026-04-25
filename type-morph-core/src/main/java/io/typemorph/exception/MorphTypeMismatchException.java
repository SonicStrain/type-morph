package io.typemorph.exception;

public class MorphTypeMismatchException extends MorphException {

    public MorphTypeMismatchException(Class<?> expectedType, Object actualValue, ClassCastException cause) {
        super("Type mismatch: registry returned a value that cannot be cast to "
              + expectedType.getName()
              + ". Actual value type: "
              + (actualValue == null ? "null" : actualValue.getClass().getName())
              + ". Ensure your registry is correctly configured.", cause);
    }
}
