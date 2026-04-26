package io.typemorph.config;

/**
 * Controls what happens when a source field value is null during reflective mapping.
 */
public enum NullFieldBehavior {

    /**
     * Leave the target field at its JVM default (null for objects, 0/false for primitives).
     * This is the default and the safest option.
     */
    SKIP,

    /**
     * Explicitly set the target field to null.
     * Invalid for primitive target fields — detected at build time and throws
     * {@link io.typemorph.exception.MorphConfigurationException}.
     */
    SET_NULL,

    /**
     * Throw {@link io.typemorph.exception.MorphFieldMappingException} when the source field is null.
     */
    THROW
}
