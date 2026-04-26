package io.typemorph.spring.autoconfigure;

import io.typemorph.TypeMorph;
import io.typemorph.config.MorphConfiguration;
import io.typemorph.mapping.MorphMapping;
import io.typemorph.reflect.AnnotationMorphScanner;
import io.typemorph.registry.DefaultMorphRegistry;
import io.typemorph.spring.aspect.TypeMorphMethodAspect;
import io.typemorph.util.GenericTypeResolver;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.ResolvableType;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.util.ArrayList;
import java.util.List;

/**
 * Spring Boot auto-configuration for type-morph.
 *
 * <p>Automatically discovers all {@code MorphMapping<S, T>} beans in the application
 * context and registers them with the {@link TypeMorph} instance.
 *
 * <p>When {@code typemorph.scan-packages} is configured, also scans those packages for
 * classes annotated with {@code @TypeMorphClass} and registers reflective field mappings.
 *
 * <p>When {@code typemorph.enable-aop=true} (default), registers the
 * {@link TypeMorphMethodAspect} bean which powers {@code @TypeMorphAccepts} on method parameters.
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
     * iterate all MorphMapping beans without failing when zero are present.
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

        // Register all MorphMapping beans found in the application context
        mappingsProvider.orderedStream().forEach(mapping -> registerMapping(typeMorph, mapping));

        // Scan packages for @TypeMorphClass annotations (reflective field mapping)
        if (!props.getScanPackages().isEmpty()) {
            List<Class<?>> annotatedClasses = scanPackages(props.getScanPackages());
            new AnnotationMorphScanner(typeMorph).scan(annotatedClasses.toArray(Class[]::new));
        }

        return typeMorph;
    }

    /**
     * Registers the {@link TypeMorphMethodAspect} bean that intercepts methods annotated
     * with {@link io.typemorph.spring.annotation.TypeMorphAccepts} and converts arguments
     * automatically via TypeMorph.
     *
     * <p>Only registered when {@code typemorph.enable-aop=true} (default) AND the
     * {@code TypeMorphMethodAspect} bean is not already present (e.g., user defined their own).
     */
    @Bean
    @ConditionalOnMissingBean(TypeMorphMethodAspect.class)
    @ConditionalOnProperty(prefix = "typemorph", name = "enable-aop", havingValue = "true",
                           matchIfMissing = true)
    public TypeMorphMethodAspect typeMorphMethodAspect(TypeMorph typeMorph) {
        return new TypeMorphMethodAspect(typeMorph);
    }

    // -------------------------------------------------------------------------
    // Package scanning for @TypeMorphClass
    // -------------------------------------------------------------------------

    /**
     * Scans the given base packages for classes annotated with {@code @TypeMorphClass}.
     * Uses Spring's {@link ClassPathScanningCandidateComponentProvider} which respects
     * the classpath index and works correctly in packaged JARs.
     */
    private List<Class<?>> scanPackages(List<String> basePackages) {
        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(
                io.typemorph.annotation.TypeMorphClass.class));

        List<Class<?>> found = new ArrayList<>();
        for (String pkg : basePackages) {
            for (var beanDef : scanner.findCandidateComponents(pkg)) {
                try {
                    found.add(Class.forName(beanDef.getBeanClassName()));
                } catch (ClassNotFoundException e) {
                    throw new IllegalStateException(
                            "TypeMorph: could not load class " + beanDef.getBeanClassName()
                            + " found during scan of package '" + pkg + "'", e);
                }
            }
        }
        return found;
    }

    // -------------------------------------------------------------------------
    // MorphMapping bean registration
    // -------------------------------------------------------------------------

    /**
     * Resolves S and T from a MorphMapping bean using Spring's ResolvableType,
     * which correctly handles CGLIB proxies created for {@code @Transactional} /
     * {@code @Scope} beans. Falls back to the core GenericTypeResolver if
     * ResolvableType cannot resolve the args.
     */
    @SuppressWarnings({"unchecked", "rawtypes"})
    private void registerMapping(TypeMorph typeMorph, MorphMapping<?, ?> mapping) {
        ResolvableType rt = ResolvableType.forClass(mapping.getClass()).as(MorphMapping.class);
        Class<?> sourceType = rt.resolveGeneric(0);
        Class<?> targetType = rt.resolveGeneric(1);

        if (sourceType == null || targetType == null) {
            // Falls back to raw reflection — will throw MorphTypeResolutionException
            // for anonymous lambdas with a clear message directing to the named-class form.
            Class<?>[] types = GenericTypeResolver.resolveTypeArguments(mapping);
            sourceType = types[0];
            targetType = types[1];
        }

        typeMorph.register((Class) sourceType, (Class) targetType, (MorphMapping) mapping);
    }
}
