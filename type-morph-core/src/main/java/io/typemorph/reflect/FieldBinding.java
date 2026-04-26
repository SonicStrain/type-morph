package io.typemorph.reflect;

import io.typemorph.config.NullFieldBehavior;

import java.lang.reflect.Field;

/**
 * Immutable record describing how one source field maps to one target field.
 * Built once at registration time by {@link ReflectiveMappingBuilder}; reused
 * for every mapping invocation.
 *
 * @param sourceField   pre-resolved, accessible source field
 * @param targetField   pre-resolved, accessible target field (null for record targets
 *                      — component position used instead)
 * @param targetComponentIndex for record targets: the index in the canonical constructor
 *                             parameter list; -1 for POJO targets
 * @param deepMap       when true, delegate value conversion to TypeMorph recursively
 * @param onNull        what to do when the source field value is null
 * @param converter     type converter to use when source and target field types differ
 */
public record FieldBinding(
        Field            sourceField,
        Field            targetField,
        int              targetComponentIndex,
        boolean          deepMap,
        NullFieldBehavior onNull,
        TypeConverter    converter
) {
    /** Returns true if this binding is for a record target (uses constructor injection). */
    public boolean isRecordTarget() {
        return targetComponentIndex >= 0;
    }

    public String sourceFieldName() {
        return sourceField.getName();
    }

    public String targetFieldName() {
        return targetField != null ? targetField.getName()
                : (sourceField.getName()); // fallback for records resolved by index
    }

    public Class<?> targetFieldType() {
        return targetField != null ? targetField.getType() : sourceField.getType();
    }
}
