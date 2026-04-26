package io.typemorph;

import io.typemorph.annotation.TypeMorphClass;
import io.typemorph.annotation.TypeMorphField;
import io.typemorph.annotation.TypeMorphIgnore;
import io.typemorph.config.NullFieldBehavior;
import io.typemorph.exception.MorphConfigurationException;
import io.typemorph.exception.MorphFieldMappingException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for annotation resolution edge cases: per-target @TypeMorphField,
 * wildcard vs. specific, NullFieldBehavior, per-target @TypeMorphIgnore,
 * and fail-fast configuration validation.
 */
class ReflectiveMappingAnnotationTest {

    // -------------------------------------------------------------------------
    // Per-target @TypeMorphField
    // -------------------------------------------------------------------------

    static class TargetA { String fieldA; }
    static class TargetB { String fieldB; }

    @TypeMorphClass(targets = {TargetA.class, TargetB.class})
    static class MultiTargetSource {
        @TypeMorphField(target = TargetA.class, name = "fieldA")
        @TypeMorphField(target = TargetB.class, name = "fieldB")
        String value;

        MultiTargetSource(String v) { this.value = v; }
    }

    @Test
    void perTargetAnnotation_differentFieldNamePerTarget() {
        TypeMorph morph = new TypeMorph().scan(MultiTargetSource.class);

        TargetA a = morph.map(new MultiTargetSource("hello"), TargetA.class);
        TargetB b = morph.map(new MultiTargetSource("world"), TargetB.class);

        assertThat(a.fieldA).isEqualTo("hello");
        assertThat(b.fieldB).isEqualTo("world");
    }

    // -------------------------------------------------------------------------
    // Wildcard @TypeMorphField (target = Void.class) applies to all targets
    // -------------------------------------------------------------------------

    static class WildcardTarget1 { String renamed; }
    static class WildcardTarget2 { String renamed; }

    @TypeMorphClass(targets = {WildcardTarget1.class, WildcardTarget2.class})
    static class WildcardSource {
        @TypeMorphField(name = "renamed") // applies to all targets
        String original;

        WildcardSource(String v) { this.original = v; }
    }

    @Test
    void wildcardAnnotation_appliesRenameToAllTargets() {
        TypeMorph morph = new TypeMorph().scan(WildcardSource.class);

        WildcardTarget1 t1 = morph.map(new WildcardSource("x"), WildcardTarget1.class);
        WildcardTarget2 t2 = morph.map(new WildcardSource("y"), WildcardTarget2.class);

        assertThat(t1.renamed).isEqualTo("x");
        assertThat(t2.renamed).isEqualTo("y");
    }

    // -------------------------------------------------------------------------
    // NullFieldBehavior.SKIP (default) — null source leaves target field untouched
    // -------------------------------------------------------------------------

    static class SkipTarget { String value = "original"; }

    @TypeMorphClass(targets = SkipTarget.class)
    static class SkipSource {
        String value = null; // explicitly null
    }

    @Test
    void nullFieldBehavior_skip_leavesTargetUntouched() {
        TypeMorph morph = new TypeMorph().scan(SkipSource.class);
        SkipTarget tgt = morph.map(new SkipSource(), SkipTarget.class);
        // SKIP → target field keeps its initialized value ("original")
        assertThat(tgt.value).isEqualTo("original");
    }

    // -------------------------------------------------------------------------
    // NullFieldBehavior.SET_NULL — null is actively written to target
    // -------------------------------------------------------------------------

    static class SetNullTarget { String value = "original"; }

    @TypeMorphClass(targets = SetNullTarget.class)
    static class SetNullSource {
        @TypeMorphField(onNull = NullFieldBehavior.SET_NULL)
        String value = null;
    }

    @Test
    void nullFieldBehavior_setNull_writesNullToTarget() {
        TypeMorph morph = new TypeMorph().scan(SetNullSource.class);
        SetNullTarget tgt = morph.map(new SetNullSource(), SetNullTarget.class);
        assertThat(tgt.value).isNull();
    }

    // -------------------------------------------------------------------------
    // NullFieldBehavior.THROW — throws MorphFieldMappingException when source is null
    // -------------------------------------------------------------------------

    static class ThrowTarget { String value; }

    @TypeMorphClass(targets = ThrowTarget.class)
    static class ThrowSource {
        @TypeMorphField(onNull = NullFieldBehavior.THROW)
        String value = null;
    }

    @Test
    void nullFieldBehavior_throw_throwsException() {
        TypeMorph morph = new TypeMorph().scan(ThrowSource.class);
        assertThatThrownBy(() -> morph.map(new ThrowSource(), ThrowTarget.class))
                .isInstanceOf(MorphFieldMappingException.class)
                .hasMessageContaining("NullFieldBehavior.THROW");
    }

    // -------------------------------------------------------------------------
    // Fail-fast: SET_NULL on a primitive target field → MorphConfigurationException
    // -------------------------------------------------------------------------

    static class PrimitiveTarget { int count; }

    @TypeMorphClass(targets = PrimitiveTarget.class)
    static class PrimitiveSource {
        @TypeMorphField(onNull = NullFieldBehavior.SET_NULL)
        Integer count = null;
    }

    @Test
    void failFast_setNullOnPrimitive_throwsAtScanTime() {
        assertThatThrownBy(() -> new TypeMorph().scan(PrimitiveSource.class))
                .isInstanceOf(MorphConfigurationException.class)
                .hasMessageContaining("SET_NULL")
                .hasMessageContaining("primitive");
    }

    // -------------------------------------------------------------------------
    // Fail-fast: final POJO target field → MorphConfigurationException
    // -------------------------------------------------------------------------

    static class FinalFieldTarget { final String name = ""; }

    @TypeMorphClass(targets = FinalFieldTarget.class)
    static class FinalFieldSource {
        String name = "test";
    }

    @Test
    void failFast_finalPojoTargetField_throwsAtScanTime() {
        assertThatThrownBy(() -> new TypeMorph().scan(FinalFieldSource.class))
                .isInstanceOf(MorphConfigurationException.class)
                .hasMessageContaining("final");
    }

    // -------------------------------------------------------------------------
    // Per-target @TypeMorphIgnore
    // -------------------------------------------------------------------------

    static class IgnoreTargetA { String value; String sensitive; }
    static class IgnoreTargetB { String value; String sensitive; }

    @TypeMorphClass(targets = {IgnoreTargetA.class, IgnoreTargetB.class})
    static class PerTargetIgnoreSource {
        String value;

        @TypeMorphIgnore(target = IgnoreTargetA.class) // only ignored for TargetA
        String sensitive;

        PerTargetIgnoreSource(String v, String s) { this.value = v; this.sensitive = s; }
    }

    @Test
    void perTargetIgnore_onlyIgnoredForSpecificTarget() {
        TypeMorph morph = new TypeMorph().scan(PerTargetIgnoreSource.class);

        IgnoreTargetA a = morph.map(new PerTargetIgnoreSource("x", "secret"), IgnoreTargetA.class);
        IgnoreTargetB b = morph.map(new PerTargetIgnoreSource("x", "secret"), IgnoreTargetB.class);

        assertThat(a.sensitive).isNull();    // ignored for TargetA
        assertThat(b.sensitive).isEqualTo("secret"); // NOT ignored for TargetB
    }
}
