package io.typemorph.exception;

public class MorphNullSourceException extends MorphException {

    public MorphNullSourceException() {
        super("Source object passed to map() is null. "
              + "Configure NullHandling.RETURN_NULL to suppress this exception.");
    }
}
