package io.typemorph.spring;

import io.typemorph.TypeMorph;
import io.typemorph.mapping.MorphMapping;
import io.typemorph.spring.autoconfigure.TypeMorphAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests verifying TypeMorph end-to-end in a Spring context.
 * Uses ApplicationContextRunner for lightweight, focused testing without
 * requiring a full @SpringBootApplication setup.
 */
class TypeMorphIntegrationTest {

    record UserEntity(Long id, String name) {}
    record UserDto(Long id, String displayName) {}

    record ProductEntity(Long id, Double price) {}
    record ProductDto(Long id, String priceFormatted) {}

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(TypeMorphAutoConfiguration.class));

    @Test
    void shouldMapUserEntityToDtoViaSpringBean() {
        MorphMapping<UserEntity, UserDto> userMapper = new MorphMapping<>() {
            @Override
            public UserDto map(UserEntity source) {
                return new UserDto(source.id(), source.name().toUpperCase());
            }
        };

        contextRunner
                .withBean("userMapper", MorphMapping.class, () -> userMapper)
                .run(ctx -> {
                    TypeMorph morph = ctx.getBean(TypeMorph.class);
                    UserDto dto = morph.map(new UserEntity(42L, "alice"));
                    assertThat(dto.id()).isEqualTo(42L);
                    assertThat(dto.displayName()).isEqualTo("ALICE");
                });
    }

    @Test
    void shouldDemonstratePolymorphicMapInSpringContext() {
        // This is the core use case: same morph.map() call for different type pairs
        MorphMapping<UserEntity, UserDto> userMapper = new MorphMapping<>() {
            @Override
            public UserDto map(UserEntity source) {
                return new UserDto(source.id(), source.name());
            }
        };
        MorphMapping<ProductEntity, ProductDto> productMapper = new MorphMapping<>() {
            @Override
            public ProductDto map(ProductEntity source) {
                return new ProductDto(source.id(), "$" + source.price());
            }
        };

        contextRunner
                .withBean("userMapper", MorphMapping.class, () -> userMapper)
                .withBean("productMapper", MorphMapping.class, () -> productMapper)
                .run(ctx -> {
                    TypeMorph morph = ctx.getBean(TypeMorph.class);

                    UserDto userDto = morph.map(new UserEntity(1L, "Bob"));
                    ProductDto productDto = morph.map(new ProductEntity(2L, 9.99));

                    assertThat(userDto.displayName()).isEqualTo("Bob");
                    assertThat(productDto.priceFormatted()).isEqualTo("$9.99");
                });
    }

    @Test
    void shouldMapListOfEntitiesViaSpringBean() {
        MorphMapping<UserEntity, UserDto> userMapper = new MorphMapping<>() {
            @Override
            public UserDto map(UserEntity source) {
                return new UserDto(source.id(), source.name());
            }
        };

        contextRunner
                .withBean("userMapper", MorphMapping.class, () -> userMapper)
                .run(ctx -> {
                    TypeMorph morph = ctx.getBean(TypeMorph.class);

                    List<UserEntity> entities = List.of(
                            new UserEntity(1L, "Alice"),
                            new UserEntity(2L, "Bob"),
                            new UserEntity(3L, "Carol")
                    );

                    List<UserDto> dtos = morph.mapList(entities, UserDto.class);
                    assertThat(dtos).hasSize(3);
                    assertThat(dtos.get(0).displayName()).isEqualTo("Alice");
                    assertThat(dtos.get(2).displayName()).isEqualTo("Carol");
                });
    }

    @Test
    void shouldReportCanMapForRegisteredSpringBeanMappers() {
        MorphMapping<UserEntity, UserDto> userMapper = new MorphMapping<>() {
            @Override
            public UserDto map(UserEntity source) {
                return new UserDto(source.id(), source.name());
            }
        };

        contextRunner
                .withBean("userMapper", MorphMapping.class, () -> userMapper)
                .run(ctx -> {
                    TypeMorph morph = ctx.getBean(TypeMorph.class);
                    assertThat(morph.canMap(UserEntity.class)).isTrue();
                    assertThat(morph.canMap(UserEntity.class, UserDto.class)).isTrue();
                    assertThat(morph.canMap(ProductEntity.class)).isFalse();
                });
    }
}
