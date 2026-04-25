package io.typemorph.exception;

import java.util.Set;
import java.util.stream.Collectors;

public class MorphAmbiguousException extends MorphException {

    private final Class<?> sourceType;
    private final Set<Class<?>> availableTargets;

    public MorphAmbiguousException(Class<?> sourceType, Set<Class<?>> availableTargets) {
        super("Ambiguous mapping for source type: " + sourceType.getName()
              + ". Multiple targets registered: ["
              + availableTargets.stream().map(Class::getName).collect(Collectors.joining(", "))
              + "]. Use map(source, Class<T> targetType) to disambiguate.");
        this.sourceType = sourceType;
        this.availableTargets = Set.copyOf(availableTargets);
    }

    public Class<?> getSourceType() {
        return sourceType;
    }

    public Set<Class<?>> getAvailableTargets() {
        return availableTargets;
    }
}
