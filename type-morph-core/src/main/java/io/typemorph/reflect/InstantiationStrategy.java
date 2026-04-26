package io.typemorph.reflect;

/**
 * Strategy for creating a new instance of a target class.
 *
 * <p>Implementations are built once at registration time by {@link ReflectiveMappingBuilder}
 * and reused for every mapping invocation. All reflection work (finding the constructor,
 * calling {@code setAccessible}) happens at build time.
 *
 * @param <T> the target type to instantiate
 */
public interface InstantiationStrategy<T> {

    /**
     * Creates a new instance of the target type.
     *
     * @param args for record targets: the full constructor argument array in component order;
     *             for POJO targets (no-arg or annotated constructor): the arguments to pass
     *             (may be empty for no-arg constructors)
     * @return a freshly created instance
     * @throws io.typemorph.exception.MorphInstantiationException if construction fails
     */
    T newInstance(Object[] args);
}
