package io.typemorph.mapping;

/**
 * Core mapping interface. Implement this (as a named class) to define how to convert
 * an instance of S into an instance of T.
 *
 * <p>Lambda usage requires explicit class registration:
 * <pre>
 *   morph.register(ClassB.class, ClassA.class, b -> new ClassA(b.getValue()));
 * </pre>
 *
 * <p>Named class usage enables auto-registration (type arguments resolved via reflection):
 * <pre>
 *   {@literal @}Component
 *   public class ClassBMapper implements MorphMapping{@literal <}ClassB, ClassA{@literal >} {
 *       public ClassA map(ClassB source) { ... }
 *   }
 * </pre>
 *
 * @param <S> the source type
 * @param <T> the target type
 */
@FunctionalInterface
public interface MorphMapping<S, T> {
    T map(S source);
}
