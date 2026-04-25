package io.typemorph;

import io.typemorph.exception.MorphTypeResolutionException;
import io.typemorph.mapping.MorphMapping;
import io.typemorph.util.GenericTypeResolver;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;

class ErrorHandlingTest {

    record Foo(String x) {}
    record Bar(String y) {}

    /**
     * Named static inner class — generic type information IS preserved.
     * GenericTypeResolver can resolve S=Foo, T=Bar from this class.
     */
    static class FooToBarMapping implements MorphMapping<Foo, Bar> {
        @Override
        public Bar map(Foo source) {
            return new Bar(source.x());
        }
    }

    @Test
    void shouldThrowMorphTypeResolutionExceptionForAnonymousLambda() {
        // Lambdas erase generic type info — resolver cannot determine S and T
        MorphMapping<Foo, Bar> lambda = source -> new Bar(source.x());
        assertThatThrownBy(() -> GenericTypeResolver.resolveTypeArguments(lambda))
                .isInstanceOf(MorphTypeResolutionException.class)
                .hasMessageContaining("register(Class<S>")
                .hasMessageContaining("Anonymous lambdas");
    }

    @Test
    void shouldResolveTypesFromNamedClass() {
        FooToBarMapping mapping = new FooToBarMapping();
        Class<?>[] types = GenericTypeResolver.resolveTypeArguments(mapping);
        assertThat(types[0]).isEqualTo(Foo.class);
        assertThat(types[1]).isEqualTo(Bar.class);
    }

    @Test
    void shouldAutoRegisterFromNamedClassWithoutExplicitTypeArgs() {
        TypeMorph morph = new TypeMorph().register(new FooToBarMapping());
        Bar result = morph.map(new Foo("hello"));
        assertThat(result.y()).isEqualTo("hello");
    }

    @Test
    void shouldReturnEmptyOptionalFromMapSafeWhenNoMapping() {
        TypeMorph morph = new TypeMorph();
        assertThat(morph.mapSafe(new Foo("x"))).isEmpty();
    }

    @Test
    void shouldReturnPresentOptionalFromMapSafe() {
        TypeMorph morph = new TypeMorph()
                .register(Foo.class, Bar.class, f -> new Bar(f.x()));
        // Store with explicit type so the lambda parameter is typed as Bar, not Object
        Optional<Bar> result = morph.mapSafe(new Foo("world"));
        assertThat(result).isPresent();
        assertThat(result.get().y()).isEqualTo("world");
    }

    @Test
    void shouldReturnEmptyOptionalFromMapSafeForNullSource() {
        TypeMorph morph = new TypeMorph()
                .register(Foo.class, Bar.class, f -> new Bar(f.x()));
        assertThat(morph.mapSafe(null)).isEmpty();
    }

    @Test
    void shouldPreserveOriginalCauseInMorphConversionException() {
        TypeMorph morph = new TypeMorph()
                .register(Foo.class, Bar.class, f -> {
                    throw new RuntimeException("original error");
                });
        assertThatThrownBy(() -> morph.map(new Foo("x")))
                .hasCauseInstanceOf(RuntimeException.class)
                .hasMessageContaining("original error");
    }
}
