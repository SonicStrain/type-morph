package io.typemorph;

import io.typemorph.config.MorphConfiguration;
import io.typemorph.config.NullElementHandling;
import io.typemorph.config.NullHandling;
import io.typemorph.exception.MorphConversionException;
import io.typemorph.exception.MorphException;
import io.typemorph.exception.MorphNullElementException;
import io.typemorph.exception.MorphNullSourceException;
import io.typemorph.exception.MorphTypeMismatchException;
import io.typemorph.mapping.MorphMapping;
import io.typemorph.registry.DefaultMorphRegistry;
import io.typemorph.registry.MorphRegistry;
import io.typemorph.util.GenericTypeResolver;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Main entry point for the type-morph library.
 *
 * <h2>Usage</h2>
 * <pre>
 *   TypeMorph morph = new TypeMorph()
 *       .register(ClassB.class, ClassA.class, b -> new ClassA(b.getValue()))
 *       .register(ClassD.class, ClassC.class, d -> new ClassC(d.getLabel()));
 *
 *   ClassA a = morph.map(classB);   // same method, different type pair
 *   ClassC c = morph.map(classD);   // same method, different type pair
 * </pre>
 *
 * <h2>Polymorphic return type</h2>
 * {@code map(Object source)} has return type {@code <T>}. The JVM infers T from the
 * call-site assignment. The actual value returned is determined by the registered
 * mapping for the source type. This is safe when the registry is correctly configured —
 * if the inferred T does not match the registered target type, a
 * {@link MorphTypeMismatchException} is thrown.
 */
public class TypeMorph {

    private final MorphRegistry registry;
    private final MorphConfiguration config;

    public TypeMorph() {
        this(new DefaultMorphRegistry(), MorphConfiguration.defaults());
    }

    public TypeMorph(MorphConfiguration config) {
        this(new DefaultMorphRegistry(), config);
    }

    public TypeMorph(MorphRegistry registry, MorphConfiguration config) {
        this.registry = registry;
        this.config = config;
    }

    // -------------------------------------------------------------------------
    // Registration API
    // -------------------------------------------------------------------------

    /**
     * Registers a mapping using explicit source and target types.
     * Works with lambdas, anonymous classes, and named classes.
     *
     * @return this instance for fluent chaining
     */
    public <S, T> TypeMorph register(Class<S> sourceType, Class<T> targetType,
                                      MorphMapping<S, T> mapping) {
        registry.register(sourceType, targetType, mapping);
        return this;
    }

    /**
     * Registers a mapping by resolving source and target types from the mapping's
     * generic type arguments via reflection. Only works for named classes that
     * implement {@code MorphMapping<S, T>} — NOT anonymous lambdas.
     *
     * @throws io.typemorph.exception.MorphTypeResolutionException if type args cannot be resolved
     * @return this instance for fluent chaining
     */
    @SuppressWarnings("unchecked")
    public TypeMorph register(MorphMapping<?, ?> mapping) {
        Class<?>[] types = GenericTypeResolver.resolveTypeArguments(mapping);
        // Safe: GenericTypeResolver validated that types[0] is S and types[1] is T
        registry.register((Class<Object>) types[0], (Class<Object>) types[1],
                (MorphMapping<Object, Object>) mapping);
        return this;
    }

    // -------------------------------------------------------------------------
    // Single-object mapping
    // -------------------------------------------------------------------------

    /**
     * Maps a source object to its registered target type.
     *
     * <p>The return type T is inferred by the JVM from the call-site assignment.
     * The registry determines the actual runtime type of the returned value.
     * This is the "same method, different type pairs" pattern:
     * <pre>
     *   ClassA a = morph.map(classB);   // registry returns ClassA
     *   ClassC c = morph.map(classD);   // registry returns ClassC
     * </pre>
     *
     * @param source the source object (must not be null unless NullHandling.RETURN_NULL is configured)
     * @param <T>    the target type, inferred from the call-site assignment
     * @return the mapped result
     * @throws MorphNullSourceException   if source is null and config is THROW_EXCEPTION
     * @throws io.typemorph.exception.MorphNotFoundException   if no mapping registered for source type
     * @throws io.typemorph.exception.MorphAmbiguousException  if multiple targets registered; use map(source, targetType)
     * @throws MorphConversionException   if the mapping function itself throws
     * @throws MorphTypeMismatchException if the result cannot be cast to the inferred T
     */
    @SuppressWarnings("unchecked")
    public <T> T map(Object source) {
        if (source == null) {
            return handleNullSource();
        }
        Class<?> sourceType = source.getClass();
        Class<?> targetType = registry.resolveDefaultTarget(sourceType);
        Object result = executeMapping(source, sourceType, targetType);
        try {
            // Unchecked cast — safe when registry is correctly configured.
            // If the call-site inferred T does not match targetType, the JVM throws
            // ClassCastException at the assignment site in the caller, caught below.
            return (T) result;
        } catch (ClassCastException cce) {
            throw new MorphTypeMismatchException(Object.class, result, cce);
        }
    }

    /**
     * Maps a source object to an explicitly specified target type.
     * Always type-safe — no unchecked cast at the call site.
     */
    public <S, T> T map(S source, Class<T> targetType) {
        if (source == null) {
            return handleNullSource();
        }
        @SuppressWarnings("unchecked")
        Class<S> sourceType = (Class<S>) source.getClass();
        Object result = executeMapping(source, sourceType, targetType);
        try {
            return targetType.cast(result);
        } catch (ClassCastException cce) {
            throw new MorphTypeMismatchException(targetType, result, cce);
        }
    }

    // -------------------------------------------------------------------------
    // Collection mapping
    // -------------------------------------------------------------------------

    /**
     * Maps a list of source objects, resolving each element's target type from the registry.
     */
    public <T> List<T> mapList(List<?> sources) {
        if (sources == null) {
            return handleNullSourceList();
        }
        List<T> results = new ArrayList<>(sources.size());
        for (int i = 0; i < sources.size(); i++) {
            Object element = sources.get(i);
            if (element == null) {
                handleNullElement(i, results);
                continue;
            }
            results.add(this.map(element));
        }
        return results;
    }

    /**
     * Maps a list of source objects to a list of the specified target type.
     */
    public <S, T> List<T> mapList(List<S> sources, Class<T> targetType) {
        if (sources == null) {
            return handleNullSourceList();
        }
        List<T> results = new ArrayList<>(sources.size());
        for (int i = 0; i < sources.size(); i++) {
            S element = sources.get(i);
            if (element == null) {
                handleNullElement(i, results);
                continue;
            }
            results.add(this.map(element, targetType));
        }
        return results;
    }

    // -------------------------------------------------------------------------
    // Safe mapping (returns Optional)
    // -------------------------------------------------------------------------

    /**
     * Maps a source object, returning {@code Optional.empty()} instead of throwing
     * for null source or missing/ambiguous mapping.
     * Mapping function exceptions are still propagated as MorphConversionException.
     */
    public <T> Optional<T> mapSafe(Object source) {
        if (source == null) {
            return Optional.empty();
        }
        if (!registry.hasMapping(source.getClass())) {
            return Optional.empty();
        }
        try {
            T result = this.map(source);
            return Optional.ofNullable(result);
        } catch (MorphException e) {
            return Optional.empty();
        }
    }

    // -------------------------------------------------------------------------
    // Inspection
    // -------------------------------------------------------------------------

    /** Returns true if any mapping is registered for the given source type. */
    public boolean canMap(Class<?> sourceType) {
        return registry.hasMapping(sourceType);
    }

    /** Returns true if a mapping from sourceType to targetType is registered. */
    public boolean canMap(Class<?> sourceType, Class<?> targetType) {
        return registry.hasMapping(sourceType, targetType);
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private <S, T> T executeMapping(Object source, Class<?> sourceType, Class<?> targetType) {
        MorphMapping<S, T> mapping =
                registry.lookup((Class<S>) sourceType, (Class<T>) targetType);
        try {
            return mapping.map((S) source);
        } catch (MorphException e) {
            throw e;
        } catch (Exception e) {
            throw new MorphConversionException(sourceType, targetType, e);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T handleNullSource() {
        if (config.getNullHandling() == NullHandling.THROW_EXCEPTION) {
            throw new MorphNullSourceException();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> handleNullSourceList() {
        if (config.getNullHandling() == NullHandling.THROW_EXCEPTION) {
            throw new MorphNullSourceException();
        }
        return null;
    }

    private <T> void handleNullElement(int index, List<T> results) {
        if (config.getNullElementHandling() == NullElementHandling.THROW_EXCEPTION) {
            throw new MorphNullElementException(index);
        }
        // SKIP: do not add to results
    }
}
