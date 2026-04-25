package io.typemorph.exception;

public class MorphNullElementException extends MorphException {

    private final int index;

    public MorphNullElementException(int index) {
        super("Null element found at index " + index
              + " in the source collection. "
              + "Configure NullElementHandling.SKIP to suppress this exception.");
        this.index = index;
    }

    public int getIndex() {
        return index;
    }
}
