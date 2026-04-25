package io.typemorph.spring.autoconfigure;

import io.typemorph.TypeMorph;
import io.typemorph.config.MorphConfiguration;
import io.typemorph.mapping.MorphMapping;
import io.typemorph.registry.DefaultMorphRegistry;
import io.typemorph.util.GenericTypeResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ResolvableType;

/**
 * Spring Boot auto-configuration for type-morph.
 *
 * <p>Automatically discovers all {@code MorphMapping<S, T>} beans in the application
 * context and registers them with the {@link TypeMorph} instance.
 *
 * <p>To override the auto-configured bean, define your own {@code @Bean} of type
 * {@link TypeMorph} — the {@code @ConditionalOnMissingBean} will back off.
 *
 * <p>Activated via:
 * {@code META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports}
 */
@AutoConfiguration
@ConditionalOnClass(TypeMorph.class)
@EnableConfigurationProperties(TypeMorphProperties.class)
public class TypeMorphAutoConfiguration {

    /**
     * Creates the primary TypeMorph bean.
     *
     * <p>Uses {@code ObjectProvider<MorphMapping<?, ?>>} with {@code orderedStream()} to
     * iterate all MorphMapping beans without failing when zero are present. This is the
     * standard Spring Boot pattern for collecting multiple optional beans of the same type.
     */
    @Bean
    @ConditionalOnMissingBean
    public TypeMorph typeMorph(TypeMorphProperties props,
                               ObjectProvider<MorphMapping<?, ?>> mappingsProvider) {
        MorphConfiguration config = MorphConfiguration.builder()
                .nullHandling(props.getNullHandling())
                .nullElementHandling(props.getNullElementHandling())
                .failOnMissingMapper(props.isFailOnMissingMapper())
                .failOnAmbiguousMapper(props.isFailOnAmbiguousMapper())
                .build();

        TypeMorph typeMorph = new TypeMorph(new DefaultMorphRegistry(), config);

        // orderedStream() iterates ALL beans of type MorphMapping<?, ?> in the context.
        // Safe when zero beans exist — produces an empty stream.
        mappingsProvider.orderedStream().forEach(mapping -> registerMapping(typeMorph, mapping));

        return typeMorph;
    }

    /**
     * Resolves S and T from a MorphMapping bean using Spring's ResolvableType,
     * which correctly handles CGLIB proxies created for @Transactional / @Scope beans.
     * Falls back to the core GenericTypeResolver if ResolvableType cannot resolve the args.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerMapping(TypeMorph typeMorph, MorphMapping<?, ?> mapping) {
        ResolvableType rt = ResolvableType.forClass(mapping.getClass()).as(MorphMapping.class);
        Class<?> sourceType = rt.resolveGeneric(0);
        Class<?> targetType = rt.resolveGeneric(1);

        if (sourceType == null || targetType == null) {
            // ResolvableType could not resolve — try raw reflection
            // This will throw MorphTypeResolutionException for true anonymous lambdas,
            // giving the user a clear error message with instructions.
            Class<?>[] types = GenericTypeResolver.resolveTypeArguments(mapping);
            sourceType = types[0];
            targetType = types[1];
        }

        typeMorph.register((Class) sourceType, (Class) targetType, (MorphMapping) mapping);
    }
}
