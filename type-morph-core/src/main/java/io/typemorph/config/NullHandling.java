package io.typemorph.config;

public enum NullHandling {
    /** Return null without throwing when a null source is passed to map(). */
    RETURN_NULL,

    /** Throw MorphNullSourceException when a null source is passed to map(). This is the default. */
    THROW_EXCEPTION
}
