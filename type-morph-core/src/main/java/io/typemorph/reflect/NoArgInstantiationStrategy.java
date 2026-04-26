package io.typemorph.reflect;

import io.typemorph.exception.MorphInstantiationException;

import java.lang.reflect.Constructor;

/**
 * Instantiates a POJO target using its no-argument constructor.
 * Field values are set individually after construction.
 *
 * @param <T> the target type
 */
final class NoArgInstantiationStrategy<T> implements InstantiationStrategy<T> {

    private final Constructor<T> constructor;

    NoArgInstantiationStrategy(Constructor<T> constructor) {
        this.constructor = constructor;
    }

    @Override
    public T newInstance(Object[] args) {
        try {
            return constructor.newInstance();
        } catch (Exception e) {
            throw new MorphInstantiationException(
                    constructor.getDeclaringClass(),
                    "no-arg constructor threw an exception", e);
        }
    }
}
