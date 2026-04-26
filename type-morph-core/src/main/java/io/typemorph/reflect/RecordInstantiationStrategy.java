package io.typemorph.reflect;

import io.typemorph.exception.MorphInstantiationException;

import java.lang.reflect.Constructor;

/**
 * Instantiates a Java record target using its canonical constructor.
 *
 * <p>Records have no settable fields — all values must be passed in the canonical
 * constructor call in the order declared by the record components.
 *
 * <p>The {@code args} array passed to {@link #newInstance} must contain one entry
 * per record component, in declaration order; unset components must be {@code null}
 * or an appropriate default.
 *
 * @param <T> the record type
 */
final class RecordInstantiationStrategy<T> implements InstantiationStrategy<T> {

    private final Constructor<T> canonicalConstructor;
    private final int            componentCount;

    RecordInstantiationStrategy(Constructor<T> canonicalConstructor, int componentCount) {
        this.canonicalConstructor = canonicalConstructor;
        this.componentCount       = componentCount;
    }

    @Override
    public T newInstance(Object[] args) {
        // args may be shorter than componentCount if some components were not mapped
        Object[] params = args.length == componentCount ? args : new Object[componentCount];
        if (args.length < componentCount) {
            System.arraycopy(args, 0, params, 0, args.length);
            // remaining slots stay null — valid for reference types; primitives will NPE
            // (prevented earlier by SET_NULL on primitive validation)
        }
        try {
            return canonicalConstructor.newInstance(params);
        } catch (Exception e) {
            throw new MorphInstantiationException(
                    canonicalConstructor.getDeclaringClass(),
                    "canonical constructor threw an exception", e);
        }
    }
}
