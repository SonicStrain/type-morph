package io.typemorph.exception;

public class MorphTypeResolutionException extends MorphException {

    public MorphTypeResolutionException(Class<?> mappingClass) {
        super("Cannot resolve generic type arguments S and T from: " + mappingClass.getName()
              + ". Anonymous lambdas and anonymous classes erase generic type information. "
              + "Use register(Class<S> sourceType, Class<T> targetType, MorphMapping<S,T> mapping) "
              + "to provide type information explicitly.");
    }
}
