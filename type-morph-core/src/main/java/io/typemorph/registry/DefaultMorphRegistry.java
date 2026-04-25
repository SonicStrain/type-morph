package io.typemorph.registry;

import io.typemorph.exception.MorphAmbiguousException;
import io.typemorph.exception.MorphDuplicateRegistrationException;
import io.typemorph.exception.MorphNotFoundException;
import io.typemorph.mapping.MorphMapping;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe registry for MorphMapping instances.
 *
 * <p>Thread-safety model:
 * - register() is synchronized to make the two-map update atomic (mappings + defaultTargets).
 * - lookup/query methods are unsynchronized — they rely on ConcurrentHashMap's visibility guarantees.
 * - This is the standard write-once (startup) / read-many (runtime) pattern.
 *
 * <p>The defaultTargets map stores Void.class as a sentinel meaning "ambiguous" (multiple
 * targets registered for this source). ConcurrentHashMap cannot store null values, so
 * Void.class serves as the sentinel.
 */
public class DefaultMorphRegistry implements MorphRegistry {

    private static final Class<?> AMBIGUOUS = Void.class;

    private final Map<Class<?>, Map<Class<?>, MorphMapping<?, ?>>> mappings =
            new ConcurrentHashMap<>();

    private final Map<Class<?>, Class<?>> defaultTargets = new ConcurrentHashMap<>();

    @Override
    public synchronized <S, T> void register(
            Class<S> sourceType,
            Class<T> targetType,
            MorphMapping<S, T> mapping) {

        Map<Class<?>, MorphMapping<?, ?>> targetMap =
                mappings.computeIfAbsent(sourceType, k -> new ConcurrentHashMap<>());

        if (targetMap.containsKey(targetType)) {
            throw new MorphDuplicateRegistrationException(sourceType, targetType);
        }

        targetMap.put(targetType, mapping);

        // First registration for this source → set as default.
        // Subsequent registrations → mark ambiguous.
        defaultTargets.merge(sourceType, targetType, (existing, incoming) -> AMBIGUOUS);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <S, T> MorphMapping<S, T> lookup(Class<S> sourceType, Class<T> targetType) {
        Map<Class<?>, MorphMapping<?, ?>> targetMap = mappings.get(sourceType);
        if (targetMap == null) {
            throw new MorphNotFoundException(sourceType, targetType);
        }
        MorphMapping<?, ?> mapping = targetMap.get(targetType);
        if (mapping == null) {
            throw new MorphNotFoundException(sourceType, targetType);
        }
        // Safe: registration API enforced the S→T contract at compile time.
        return (MorphMapping<S, T>) mapping;
    }

    @Override
    public Class<?> resolveDefaultTarget(Class<?> sourceType) {
        if (!mappings.containsKey(sourceType)) {
            throw new MorphNotFoundException(sourceType);
        }
        Class<?> defaultTarget = defaultTargets.get(sourceType);
        if (defaultTarget == null || defaultTarget == AMBIGUOUS) {
            throw new MorphAmbiguousException(sourceType, getTargetsFor(sourceType));
        }
        return defaultTarget;
    }

    @Override
    public boolean hasMapping(Class<?> sourceType) {
        return mappings.containsKey(sourceType);
    }

    @Override
    public boolean hasMapping(Class<?> sourceType, Class<?> targetType) {
        Map<Class<?>, MorphMapping<?, ?>> targetMap = mappings.get(sourceType);
        return targetMap != null && targetMap.containsKey(targetType);
    }

    @Override
    public Set<Class<?>> getTargetsFor(Class<?> sourceType) {
        Map<Class<?>, MorphMapping<?, ?>> targetMap = mappings.get(sourceType);
        if (targetMap == null) return Collections.emptySet();
        return Collections.unmodifiableSet(targetMap.keySet());
    }
}
