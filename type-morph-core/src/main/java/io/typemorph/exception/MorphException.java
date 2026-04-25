package io.typemorph.exception;

public abstract class MorphException extends RuntimeException {

    protected MorphException(String message) {
        super(message);
    }

    protected MorphException(String message, Throwable cause) {
        super(message, cause);
    }
}
