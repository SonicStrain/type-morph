package io.typemorph.util;

import io.typemorph.exception.MorphTypeResolutionException;
import io.typemorph.mapping.MorphMapping;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Resolves generic type arguments S and T from a MorphMapping implementation
 * using Java reflection.
 *
 * <p>Works for named classes that implement MorphMapping&lt;S, T&gt; explicitly.
 * Does NOT work for anonymous lambdas — the compiler erases type args for lambdas,
 * so the interface appears as a raw type. In that case, use
 * register(Class&lt;S&gt;, Class&lt;T&gt;, MorphMapping&lt;S, T&gt;) instead.
 */
public final class GenericTypeResolver {

    private GenericTypeResolver() {}

    /**
     * Resolves [sourceType, targetType] from the given MorphMapping instance.
     *
     * @throws MorphTypeResolutionException if type arguments cannot be determined
     */
    public static Class<?>[] resolveTypeArguments(MorphMapping<?, ?> mapping) {
        return resolveFromClass(mapping.getClass());
    }

    private static Class<?>[] resolveFromClass(Class<?> clazz) {
        // Walk directly implemented interfaces first
        for (Type iface : clazz.getGenericInterfaces()) {
            Class<?>[] result = tryExtractFromType(iface);
            if (result != null) return result;
        }

        // Walk up the superclass chain
        Type superType = clazz.getGenericSuperclass();
        if (superType != null) {
            if (superType instanceof ParameterizedType pt) {
                Class<?>[] result = tryExtractFromType(pt);
                if (result != null) return result;
            }
            Class<?> superRaw = superType instanceof ParameterizedType pt
                    ? (Class<?>) pt.getRawType()
                    : (superType instanceof Class<?> c ? c : null);
            if (superRaw != null && superRaw != Object.class) {
                try {
                    return resolveFromClass(superRaw);
                } catch (MorphTypeResolutionException ignored) {
                    // Fall through to throw below
                }
            }
        }

        throw new MorphTypeResolutionException(clazz);
    }

    private static Class<?>[] tryExtractFromType(Type type) {
        if (!(type instanceof ParameterizedType pt)) return null;
        if (!(pt.getRawType() instanceof Class<?> raw)) return null;
        if (!MorphMapping.class.isAssignableFrom(raw)) return null;

        Type[] args = pt.getActualTypeArguments();
        if (args.length != 2) return null;

        Class<?> s = toRawClass(args[0]);
        Class<?> t = toRawClass(args[1]);
        if (s == null || t == null) return null;

        return new Class<?>[]{ s, t };
    }

    private static Class<?> toRawClass(Type type) {
        if (type instanceof Class<?> c) return c;
        if (type instanceof ParameterizedType pt && pt.getRawType() instanceof Class<?> c) return c;
        return null; // wildcard, TypeVariable, GenericArrayType — cannot resolve
    }
}
