package io.typemorph;

import io.typemorph.mapping.BidirectionalMorphMapping;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

class BidirectionalMorphTest {

    record EntityA(String value) {}
    record EntityB(String data) {}

    @Test
    void shouldMapForwardAndReverseViaAnonymousClass() {
        BidirectionalMorphMapping<EntityA, EntityB> biMapper = new BidirectionalMorphMapping<>() {
            @Override
            public EntityB forward(EntityA source) {
                return new EntityB(source.value().toUpperCase());
            }

            @Override
            public EntityA reverse(EntityB source) {
                return new EntityA(source.data().toLowerCase());
            }
        };

        TypeMorph morph = new TypeMorph()
                .register(EntityA.class, EntityB.class, biMapper.forwardMapping())
                .register(EntityB.class, EntityA.class, biMapper.reverseMapping());

        EntityB b = morph.map(new EntityA("hello"), EntityB.class);
        assertThat(b.data()).isEqualTo("HELLO");

        EntityA a = morph.map(new EntityB("WORLD"), EntityA.class);
        assertThat(a.value()).isEqualTo("world");
    }

    @Test
    void shouldMapForwardViaInferredType() {
        BidirectionalMorphMapping<EntityA, EntityB> biMapper = new BidirectionalMorphMapping<>() {
            @Override
            public EntityB forward(EntityA source) {
                return new EntityB(source.value() + "_mapped");
            }

            @Override
            public EntityA reverse(EntityB source) {
                return new EntityA(source.data().replace("_mapped", ""));
            }
        };

        TypeMorph morph = new TypeMorph()
                .register(EntityA.class, EntityB.class, biMapper.forwardMapping());

        // Type inferred from assignment — the polymorphic pattern
        EntityB result = morph.map(new EntityA("test"));
        assertThat(result.data()).isEqualTo("test_mapped");
    }
}
