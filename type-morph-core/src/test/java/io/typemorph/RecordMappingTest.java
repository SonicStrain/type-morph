package io.typemorph;

import io.typemorph.annotation.TypeMorphClass;
import io.typemorph.annotation.TypeMorphField;
import io.typemorph.annotation.TypeMorphIgnore;
import io.typemorph.exception.MorphConfigurationException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

/**
 * Tests for Java record targets — records must be instantiated via their canonical
 * constructor since their fields are final.
 */
class RecordMappingTest {

    // -------------------------------------------------------------------------
    // Basic record target
    // -------------------------------------------------------------------------

    record PersonRecord(String name, int age) {}

    @TypeMorphClass(targets = PersonRecord.class)
    static class PersonPojo {
        String name;
        int    age;
        PersonPojo(String n, int a) { name = n; age = a; }
    }

    @Test
    void pojo_to_record_basicFields() {
        TypeMorph morph = new TypeMorph().scan(PersonPojo.class);
        PersonRecord r = morph.map(new PersonPojo("Alice", 30), PersonRecord.class);
        assertThat(r.name()).isEqualTo("Alice");
        assertThat(r.age()).isEqualTo(30);
    }

    // -------------------------------------------------------------------------
    // Renamed field to record component
    // -------------------------------------------------------------------------

    record FullNameRecord(String fullName, int score) {}

    @TypeMorphClass(targets = FullNameRecord.class)
    static class AnnotatedPojo {
        @TypeMorphField(name = "fullName")
        String name;
        int score;
        AnnotatedPojo(String n, int s) { name = n; score = s; }
    }

    @Test
    void pojo_to_record_renamedField() {
        TypeMorph morph = new TypeMorph().scan(AnnotatedPojo.class);
        FullNameRecord r = morph.map(new AnnotatedPojo("Bob", 99), FullNameRecord.class);
        assertThat(r.fullName()).isEqualTo("Bob");
        assertThat(r.score()).isEqualTo(99);
    }

    // -------------------------------------------------------------------------
    // Record component not present in source — stays null/default
    // -------------------------------------------------------------------------

    record PartialRecord(String name, String extra) {}

    @TypeMorphClass(targets = PartialRecord.class)
    static class PartialSource {
        String name;
        PartialSource(String n) { name = n; }
    }

    @Test
    void record_unmappedComponent_isNull() {
        TypeMorph morph = new TypeMorph().scan(PartialSource.class);
        PartialRecord r = morph.map(new PartialSource("Carol"), PartialRecord.class);
        assertThat(r.name()).isEqualTo("Carol");
        assertThat(r.extra()).isNull();
    }

    // -------------------------------------------------------------------------
    // Ignored field for record target
    // -------------------------------------------------------------------------

    record SecureRecord(String name) {}

    @TypeMorphClass(targets = SecureRecord.class)
    static class SecureSource {
        String name;

        @TypeMorphIgnore(target = SecureRecord.class)
        String password;

        SecureSource(String n, String p) { name = n; password = p; }
    }

    @Test
    void record_ignoredField_notMapped() {
        TypeMorph morph = new TypeMorph().scan(SecureSource.class);
        SecureRecord r = morph.map(new SecureSource("Dave", "secret"), SecureRecord.class);
        assertThat(r.name()).isEqualTo("Dave");
        // password has no corresponding component in SecureRecord — silently skipped
    }

    // -------------------------------------------------------------------------
    // Bidirectional: record → POJO (reverse)
    // -------------------------------------------------------------------------

    @TypeMorphClass(targets = PersonRecord.class, bidirectional = true)
    static class PersonPojoForBidi {
        String name;
        int    age;
        PersonPojoForBidi() {}
        PersonPojoForBidi(String n, int a) { name = n; age = a; }
    }

    @Test
    void bidirectional_record_to_pojo() {
        TypeMorph morph = new TypeMorph().scan(PersonPojoForBidi.class);

        // Forward: POJO → record
        PersonRecord rec = morph.map(new PersonPojoForBidi("Eve", 25), PersonRecord.class);
        assertThat(rec.name()).isEqualTo("Eve");

        // Reverse: record → POJO
        PersonPojoForBidi pojo = morph.map(rec, PersonPojoForBidi.class);
        assertThat(pojo.name).isEqualTo("Eve");
        assertThat(pojo.age).isEqualTo(25);
    }

    // -------------------------------------------------------------------------
    // Record source → POJO target (source is a record)
    // -------------------------------------------------------------------------

    record OrderRecord(String orderId, double amount) {}

    static class OrderDto {
        String orderId;
        double amount;
    }

    @TypeMorphClass(targets = OrderDto.class)
    record AnnotatedOrderRecord(String orderId, double amount) {}

    @Test
    void record_as_source_mapsToPojo() {
        TypeMorph morph = new TypeMorph().scan(AnnotatedOrderRecord.class);
        AnnotatedOrderRecord rec = new AnnotatedOrderRecord("ORD-1", 99.99);
        OrderDto dto = morph.map(rec, OrderDto.class);
        assertThat(dto.orderId).isEqualTo("ORD-1");
        assertThat(dto.amount).isCloseTo(99.99, within(0.001));
    }

    // -------------------------------------------------------------------------
    // Fail-fast: no-arg constructor required for POJO when @TypeMorphConstructor absent
    // -------------------------------------------------------------------------

    static class NoDefaultCtorTarget {
        String name;
        NoDefaultCtorTarget(String n) { name = n; } // no no-arg
    }

    @TypeMorphClass(targets = NoDefaultCtorTarget.class)
    static class AnySource {
        String name = "x";
    }

    @Test
    void failFast_noDefaultConstructor_throwsAtScanTime() {
        assertThatThrownBy(() -> new TypeMorph().scan(AnySource.class))
                .isInstanceOf(MorphConfigurationException.class)
                .hasMessageContaining("no-arg constructor");
    }
}
