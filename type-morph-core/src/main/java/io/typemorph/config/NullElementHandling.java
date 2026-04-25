package io.typemorph.config;

public enum NullElementHandling {
    /** Skip null elements during collection mapping. Output list may be shorter than input. */
    SKIP,

    /** Throw MorphNullElementException when a null element is found in a collection. This is the default. */
    THROW_EXCEPTION
}
