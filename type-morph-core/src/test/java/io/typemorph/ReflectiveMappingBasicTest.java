package io.typemorph;

import io.typemorph.annotation.TypeMorphClass;
import io.typemorph.annotation.TypeMorphField;
import io.typemorph.annotation.TypeMorphIgnore;
import io.typemorph.annotation.TypeMorphConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for basic reflective field mapping: same-name fields, annotated name remapping,
 * @TypeMorphIgnore, and no-arg constructor instantiation.
 */
class ReflectiveMappingBasicTest {

    TypeMorph morph;

    @BeforeEach
    void setUp() {
        morph = new TypeMorph().scan(SourcePojo.class, AnnotatedSource.class,
                                     NoArgSource.class, InheritedSource.class);
    }

    // -------------------------------------------------------------------------
    // Fixture classes
    // -------------------------------------------------------------------------

    @TypeMorphClass(targets = TargetPojo.class)
    static class SourcePojo {
        String name;
        int    age;
        String ignored;

        SourcePojo(String name, int age, String ignored) {
            this.name = name; this.age = age; this.ignored = ignored;
        }
    }

    static class TargetPojo {
        String name;
        int    age;
        String ignored;
    }

    @TypeMorphClass(targets = RenamedTarget.class)
    static class AnnotatedSource {
        @TypeMorphField(name = "fullName")
        String name;

        @TypeMorphIgnore
        String secret;

        int score;

        AnnotatedSource(String name, String secret, int score) {
            this.name = name; this.secret = secret; this.score = score;
        }
    }

    static class RenamedTarget {
        String fullName;
        String secret;   // should be null (ignored)
        int    score;
    }

    @TypeMorphClass(targets = TargetPojo.class)
    static class NoArgSource {
        String name = "default";
        int    age  = 0;

        NoArgSource() {}
    }

    // Superclass has a field that should be picked up
    static class BasePojo {
        String baseField;
    }

    @TypeMorphClass(targets = TargetWithBase.class)
    static class InheritedSource extends BasePojo {
        String ownField;

        InheritedSource(String base, String own) {
            this.baseField = base; this.ownField = own;
        }
    }

    static class TargetWithBase {
        String baseField;
        String ownField;
    }

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void sameName_fieldsAreMapped() {
        SourcePojo src = new SourcePojo("Alice", 30, "x");
        TargetPojo tgt = morph.map(src, TargetPojo.class);

        assertThat(tgt.name).isEqualTo("Alice");
        assertThat(tgt.age).isEqualTo(30);
        assertThat(tgt.ignored).isEqualTo("x");
    }

    @Test
    void annotatedName_remapsField() {
        AnnotatedSource src = new AnnotatedSource("Bob", "hidden", 99);
        RenamedTarget   tgt = morph.map(src, RenamedTarget.class);

        assertThat(tgt.fullName).isEqualTo("Bob");
        assertThat(tgt.score).isEqualTo(99);
    }

    @Test
    void typeMorphIgnore_fieldIsSkipped() {
        AnnotatedSource src = new AnnotatedSource("Bob", "should-not-appear", 10);
        RenamedTarget   tgt = morph.map(src, RenamedTarget.class);

        assertThat(tgt.secret).isNull(); // ignored → target stays at default null
    }

    @Test
    void noArgSource_canBeScanned() {
        NoArgSource src = new NoArgSource();
        TargetPojo  tgt = morph.map(src, TargetPojo.class);

        assertThat(tgt.name).isEqualTo("default");
        assertThat(tgt.age).isEqualTo(0);
    }

    @Test
    void inheritedFields_areMapped() {
        InheritedSource src = new InheritedSource("fromBase", "fromOwn");
        TargetWithBase  tgt = morph.map(src, TargetWithBase.class);

        assertThat(tgt.baseField).isEqualTo("fromBase");
        assertThat(tgt.ownField).isEqualTo("fromOwn");
    }

    @Test
    void ensureType_returnsDirectlyIfAlreadyCorrectType() {
        TargetPojo existing = new TargetPojo();
        existing.name = "direct";
        TargetPojo result = morph.ensureType(existing, TargetPojo.class);
        assertThat(result).isSameAs(existing);
    }

    @Test
    void ensureType_convertsWhenTypeDiffers() {
        SourcePojo src = new SourcePojo("Charlie", 25, "");
        TargetPojo tgt = morph.ensureType(src, TargetPojo.class);
        assertThat(tgt.name).isEqualTo("Charlie");
    }
}
