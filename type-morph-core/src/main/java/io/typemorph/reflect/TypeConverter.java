package io.typemorph.reflect;

/**
 * Converts a value from one type to another during reflective field mapping.
 * Implementations are called only when the source field type is not directly
 * assignable to the target field type.
 */
public interface TypeConverter {

    /**
     * Returns true if this converter can handle the given type pair.
     */
    boolean canConvert(Class<?> sourceType, Class<?> targetType);

    /**
     * Converts {@code value} (which may be null) to the target type.
     *
     * @throws io.typemorph.exception.MorphFieldTypeException if conversion is not possible
     */
    Object convert(Object value, Class<?> targetType);
}
