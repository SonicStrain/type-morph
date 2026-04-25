package io.typemorph.mapping;

import java.util.Objects;

/**
 * Immutable record holding a source-to-target type pair.
 * Used as a composite registry key.
 */
public record TypePair<S, T>(Class<S> sourceType, Class<T> targetType) {

    public TypePair {
        Objects.requireNonNull(sourceType, "sourceType must not be null");
        Objects.requireNonNull(targetType, "targetType must not be null");
    }

    @Override
    public String toString() {
        return sourceType.getName() + " -> " + targetType.getName();
    }
}
