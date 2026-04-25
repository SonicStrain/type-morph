package io.typemorph.spring;

import io.typemorph.TypeMorph;
import io.typemorph.config.NullHandling;
import io.typemorph.mapping.MorphMapping;
import io.typemorph.spring.autoconfigure.TypeMorphAutoConfiguration;
import io.typemorph.spring.autoconfigure.TypeMorphProperties;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.*;

class TypeMorphAutoConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TypeMorphAutoConfiguration.class));

    @Test
    void shouldCreateTypeMorphBeanAutomatically() {
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(TypeMorph.class);
            assertThat(ctx).hasSingleBean(TypeMorphProperties.class);
        });
    }

    @Test
    void shouldNotOverrideUserDefinedTypeMorphBean() {
        contextRunner
                .withBean(TypeMorph.class, TypeMorph::new)
                .run(ctx -> assertThat(ctx).hasSingleBean(TypeMorph.class));
    }

    @Test
    void shouldBindNullHandlingPropertyFromYaml() {
        contextRunner
                .withPropertyValues("typemorph.null-handling=RETURN_NULL")
                .run(ctx -> {
                    TypeMorphProperties props = ctx.getBean(TypeMorphProperties.class);
                    assertThat(props.getNullHandling()).isEqualTo(NullHandling.RETURN_NULL);
                });
    }

    @Test
    void shouldAutoRegisterNamedMorphMappingBeans() {
        record Src(String val) {}
        record Dst(String out) {}

        // Named anonymous class (not a lambda) — type args are preserved
        MorphMapping<Src, Dst> namedMapper = new MorphMapping<Src, Dst>() {
            @Override
            public Dst map(Src source) {
                return new Dst(source.val().toUpperCase());
            }
        };

        contextRunner
                .withBean("srcToDstMapper", MorphMapping.class, () -> namedMapper)
                .run(ctx -> {
                    TypeMorph typeMorph = ctx.getBean(TypeMorph.class);
                    assertThat(typeMorph.canMap(Src.class)).isTrue();
                    assertThat(typeMorph.canMap(Src.class, Dst.class)).isTrue();
                });
    }

    @Test
    void shouldWorkWithNoMorphMappingBeansRegistered() {
        // Must not throw even when zero MorphMapping beans are in context
        contextRunner.run(ctx -> {
            assertThat(ctx).hasSingleBean(TypeMorph.class);
            assertThat(ctx).doesNotHaveBean(MorphMapping.class);
        });
    }
}
