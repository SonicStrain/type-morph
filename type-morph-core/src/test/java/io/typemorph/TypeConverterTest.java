package io.typemorph;

import io.typemorph.annotation.TypeMorphClass;
import io.typemorph.annotation.TypeMorphField;
import io.typemorph.exception.MorphConfigurationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for the built-in {@link io.typemorph.reflect.DefaultTypeConverter}:
 * primitive ↔ wrapper, numeric widening/narrowing, String↔numeric, Enum↔String.
 * Also verifies fail-fast behaviour when types are incompatible without deepMap.
 */
class TypeConverterTest {

    // -------------------------------------------------------------------------
    // int ↔ long (widening)
    // -------------------------------------------------------------------------

    static class LongSource { long value; LongSource(long v) { value = v; } }
    static class IntTarget   { int  value; }

    @TypeMorphClass(targets = IntTarget.class)
    static class IntToLongSource { int value; IntToLongSource(int v) { value = v; } }

    static class LongTarget { long value; }

    @TypeMorphClass(targets = LongTarget.class)
    static class LongToLongSource { int value; LongToLongSource(int v) { value = v; } }

    @Test
    void intToLong_widening() {
        TypeMorph morph = new TypeMorph().scan(LongToLongSource.class);
        LongTarget tgt = morph.map(new LongToLongSource(42), LongTarget.class);
        assertThat(tgt.value).isEqualTo(42L);
    }

    // -------------------------------------------------------------------------
    // String → int, long, double, float, short, byte
    // -------------------------------------------------------------------------

    static class StringSource { String value; StringSource(String v) { value = v; } }

    static class IntTargetStr  { int    value; }
    static class LongTargetStr { long   value; }
    static class DblTargetStr  { double value; }

    @TypeMorphClass(targets = IntTargetStr.class)
    static class StrToIntSource { String value; StrToIntSource(String v) { value = v; } }

    @TypeMorphClass(targets = LongTargetStr.class)
    static class StrToLongSource { String value; StrToLongSource(String v) { value = v; } }

    @TypeMorphClass(targets = DblTargetStr.class)
    static class StrToDblSource { String value; StrToDblSource(String v) { value = v; } }

    @Test void stringToInt()    {
        IntTargetStr t = new TypeMorph().scan(StrToIntSource.class).map(new StrToIntSource("7"), IntTargetStr.class);
        assertThat(t.value).isEqualTo(7);
    }
    @Test void stringToLong()   {
        LongTargetStr t = new TypeMorph().scan(StrToLongSource.class).map(new StrToLongSource("999"), LongTargetStr.class);
        assertThat(t.value).isEqualTo(999L);
    }
    @Test void stringToDouble() {
        DblTargetStr t = new TypeMorph().scan(StrToDblSource.class).map(new StrToDblSource("3.14"), DblTargetStr.class);
        assertThat(t.value).isCloseTo(3.14, within(0.001));
    }

    // -------------------------------------------------------------------------
    // Numeric → String
    // -------------------------------------------------------------------------

    static class StrTarget { String value; }

    @TypeMorphClass(targets = StrTarget.class)
    static class IntToStrSource { int value; IntToStrSource(int v) { value = v; } }

    @Test
    void intToString() {
        StrTarget t = new TypeMorph().scan(IntToStrSource.class).map(new IntToStrSource(123), StrTarget.class);
        assertThat(t.value).isEqualTo("123");
    }

    // -------------------------------------------------------------------------
    // Enum ↔ String
    // -------------------------------------------------------------------------

    enum Color { RED, GREEN, BLUE }

    static class EnumSource { Color color; EnumSource(Color c) { color = c; } }
    static class EnumStrTarget { String color; }

    @TypeMorphClass(targets = EnumStrTarget.class)
    static class ColorToStrSource { Color color; ColorToStrSource(Color c) { color = c; } }

    static class EnumTarget { Color color; }

    @TypeMorphClass(targets = EnumTarget.class)
    static class StrToColorSource { String color; StrToColorSource(String c) { color = c; } }

    @Test
    void enumToString() {
        EnumStrTarget t = new TypeMorph().scan(ColorToStrSource.class).map(new ColorToStrSource(Color.GREEN), EnumStrTarget.class);
        assertThat(t.color).isEqualTo("GREEN");
    }

    @Test
    void stringToEnum() {
        EnumTarget t = new TypeMorph().scan(StrToColorSource.class).map(new StrToColorSource("BLUE"), EnumTarget.class);
        assertThat(t.color).isEqualTo(Color.BLUE);
    }

    // -------------------------------------------------------------------------
    // Boolean ↔ String
    // -------------------------------------------------------------------------

    static class BoolStrTarget { String value; }
    static class BoolTarget    { boolean value; }

    @TypeMorphClass(targets = BoolStrTarget.class)
    static class BoolToStrSource { boolean value; BoolToStrSource(boolean v) { value = v; } }

    @TypeMorphClass(targets = BoolTarget.class)
    static class StrToBoolSource { String value; StrToBoolSource(String v) { value = v; } }

    @Test void booleanToString() {
        BoolStrTarget t = new TypeMorph().scan(BoolToStrSource.class).map(new BoolToStrSource(true), BoolStrTarget.class);
        assertThat(t.value).isEqualTo("true");
    }

    @Test void stringToBoolean() {
        BoolTarget t = new TypeMorph().scan(StrToBoolSource.class).map(new StrToBoolSource("true"), BoolTarget.class);
        assertThat(t.value).isTrue();
    }

    // -------------------------------------------------------------------------
    // Incompatible types without deepMap → MorphConfigurationException at scan time
    // -------------------------------------------------------------------------

    static class Unrelated {}
    static class UnrelatedTarget { Unrelated value; }

    @TypeMorphClass(targets = UnrelatedTarget.class)
    static class IncompatibleSource { String value; IncompatibleSource(String v) { value = v; } }

    @Test
    void incompatibleTypes_withoutDeepMap_failsAtScanTime() {
        assertThatThrownBy(() -> new TypeMorph().scan(IncompatibleSource.class))
                .isInstanceOf(MorphConfigurationException.class)
                .hasMessageContaining("cannot be converted")
                .hasMessageContaining("deepMap");
    }
}
