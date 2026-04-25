package io.typemorph;

import io.typemorph.config.MorphConfiguration;
import io.typemorph.config.NullHandling;
import io.typemorph.exception.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class TypeMorphTest {

    record UserEntity(Long id, String firstName, String lastName) {}
    record UserDto(Long id, String fullName) {}
    record OrderEntity(Long id, String description) {}
    record OrderDto(Long orderId, String desc) {}

    private TypeMorph morph;

    @BeforeEach
    void setUp() {
        morph = new TypeMorph();
        morph.register(UserEntity.class, UserDto.class,
                user -> new UserDto(user.id(), user.firstName() + " " + user.lastName()));
    }

    @Test
    void shouldMapSingleObject() {
        UserEntity entity = new UserEntity(1L, "John", "Doe");
        UserDto dto = morph.map(entity);
        assertThat(dto.id()).isEqualTo(1L);
        assertThat(dto.fullName()).isEqualTo("John Doe");
    }

    @Test
    void shouldMapWithExplicitTargetType() {
        UserEntity entity = new UserEntity(2L, "Jane", "Smith");
        UserDto dto = morph.map(entity, UserDto.class);
        assertThat(dto.fullName()).isEqualTo("Jane Smith");
    }

    @Test
    void shouldThrowMorphNotFoundExceptionForUnregisteredType() {
        assertThatThrownBy(() -> morph.map(new OrderEntity(1L, "test")))
                .isInstanceOf(MorphNotFoundException.class)
                .hasMessageContaining("OrderEntity");
    }

    @Test
    void shouldThrowMorphNullSourceExceptionByDefault() {
        assertThatThrownBy(() -> morph.map(null))
                .isInstanceOf(MorphNullSourceException.class);
    }

    @Test
    void shouldReturnNullForNullSourceWhenConfiguredAsReturnNull() {
        TypeMorph nullOk = new TypeMorph(MorphConfiguration.builder()
                .nullHandling(NullHandling.RETURN_NULL)
                .build());
        UserDto result = nullOk.map(null);
        assertThat(result).isNull();
    }

    @Test
    void shouldThrowDuplicateRegistrationExceptionOnDuplicate() {
        assertThatThrownBy(() ->
                morph.register(UserEntity.class, UserDto.class, u -> new UserDto(0L, "dup")))
                .isInstanceOf(MorphDuplicateRegistrationException.class);
    }

    @Test
    void shouldThrowMorphAmbiguousExceptionWhenMultipleTargetsAndNoExplicitType() {
        record AltDto(String name) {}
        morph.register(UserEntity.class, AltDto.class, u -> new AltDto(u.firstName()));
        assertThatThrownBy(() -> morph.map(new UserEntity(1L, "A", "B")))
                .isInstanceOf(MorphAmbiguousException.class)
                .hasMessageContaining("disambiguate");
    }

    @Test
    void shouldUseExplicitTargetTypeWhenAmbiguous() {
        record AltDto(String name) {}
        morph.register(UserEntity.class, AltDto.class, u -> new AltDto(u.firstName()));
        AltDto result = morph.map(new UserEntity(1L, "Alice", "X"), AltDto.class);
        assertThat(result.name()).isEqualTo("Alice");
    }

    @Test
    void shouldReturnTrueForCanMapWhenRegistered() {
        assertThat(morph.canMap(UserEntity.class)).isTrue();
        assertThat(morph.canMap(UserEntity.class, UserDto.class)).isTrue();
    }

    @Test
    void shouldReturnFalseForCanMapWhenNotRegistered() {
        assertThat(morph.canMap(OrderEntity.class)).isFalse();
        assertThat(morph.canMap(UserEntity.class, OrderDto.class)).isFalse();
    }

    @Test
    void shouldWrapMappingExceptionsInMorphConversionException() {
        morph.register(OrderEntity.class, OrderDto.class, o -> {
            throw new IllegalStateException("mapping broke");
        });
        assertThatThrownBy(() -> morph.map(new OrderEntity(1L, "x")))
                .isInstanceOf(MorphConversionException.class)
                .hasCauseInstanceOf(IllegalStateException.class)
                .hasMessageContaining("mapping broke");
    }

    @Test
    void shouldSupportFluentMethodChaining() {
        TypeMorph chained = new TypeMorph()
                .register(UserEntity.class, UserDto.class,
                        u -> new UserDto(u.id(), u.firstName()))
                .register(OrderEntity.class, OrderDto.class,
                        o -> new OrderDto(o.id(), o.description()));
        assertThat(chained.canMap(UserEntity.class)).isTrue();
        assertThat(chained.canMap(OrderEntity.class)).isTrue();
    }

    @Test
    void shouldDemonstratePolymorphicMapMethod() {
        // This is the core use case: same method, different type pairs
        morph.register(OrderEntity.class, OrderDto.class,
                o -> new OrderDto(o.id(), o.description()));

        UserEntity userEntity = new UserEntity(1L, "John", "Doe");
        OrderEntity orderEntity = new OrderEntity(99L, "Widget");

        UserDto userDto = morph.map(userEntity);   // returns UserDto
        OrderDto orderDto = morph.map(orderEntity); // returns OrderDto — same method!

        assertThat(userDto.fullName()).isEqualTo("John Doe");
        assertThat(orderDto.orderId()).isEqualTo(99L);
        assertThat(orderDto.desc()).isEqualTo("Widget");
    }
}
