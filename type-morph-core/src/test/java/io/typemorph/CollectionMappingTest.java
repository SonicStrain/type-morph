package io.typemorph;

import io.typemorph.config.MorphConfiguration;
import io.typemorph.config.NullElementHandling;
import io.typemorph.exception.MorphNullElementException;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class CollectionMappingTest {

    record Src(int val) {}
    record Dst(int doubled) {}

    private TypeMorph morphWith(MorphConfiguration config) {
        return new TypeMorph(config)
                .register(Src.class, Dst.class, s -> new Dst(s.val() * 2));
    }

    @Test
    void shouldMapListOfObjects() {
        TypeMorph morph = morphWith(MorphConfiguration.defaults());
        List<Dst> results = morph.mapList(List.of(new Src(1), new Src(2), new Src(3)));
        assertThat(results).hasSize(3);
        assertThat(results.get(0).doubled()).isEqualTo(2);
        assertThat(results.get(1).doubled()).isEqualTo(4);
        assertThat(results.get(2).doubled()).isEqualTo(6);
    }

    @Test
    void shouldMapListWithExplicitTargetType() {
        TypeMorph morph = morphWith(MorphConfiguration.defaults());
        List<Dst> results = morph.mapList(List.of(new Src(5), new Src(10)), Dst.class);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).doubled()).isEqualTo(10);
        assertThat(results.get(1).doubled()).isEqualTo(20);
    }

    @Test
    void shouldThrowOnNullElementByDefault() {
        TypeMorph morph = morphWith(MorphConfiguration.defaults());
        List<Src> withNull = Arrays.asList(new Src(1), null, new Src(3));
        assertThatThrownBy(() -> morph.mapList(withNull))
                .isInstanceOf(MorphNullElementException.class)
                .hasMessageContaining("index 1");
    }

    @Test
    void shouldSkipNullElementsWhenConfigured() {
        TypeMorph morph = morphWith(MorphConfiguration.builder()
                .nullElementHandling(NullElementHandling.SKIP)
                .build());
        List<Src> withNulls = Arrays.asList(new Src(1), null, new Src(3), null);
        List<Dst> results = morph.mapList(withNulls);
        assertThat(results).hasSize(2);
        assertThat(results.get(0).doubled()).isEqualTo(2);
        assertThat(results.get(1).doubled()).isEqualTo(6);
    }

    @Test
    void shouldReturnEmptyListForEmptyInput() {
        TypeMorph morph = morphWith(MorphConfiguration.defaults());
        List<Dst> results = morph.mapList(List.of());
        assertThat(results).isEmpty();
    }

    @Test
    void shouldMapListWithExplicitTypeAndSkipNulls() {
        TypeMorph morph = morphWith(MorphConfiguration.builder()
                .nullElementHandling(NullElementHandling.SKIP)
                .build());
        List<Src> withNull = Arrays.asList(null, new Src(7));
        List<Dst> results = morph.mapList(withNull, Dst.class);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).doubled()).isEqualTo(14);
    }
}
