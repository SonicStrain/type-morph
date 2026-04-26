package io.typemorph.reflect;

import io.typemorph.exception.MorphFieldTypeException;

import java.util.Map;
import java.util.Set;

/**
 * Built-in type converter covering the most common field type incompatibilities:
 *
 * <ul>
 *   <li>Primitive ↔ wrapper (int ↔ Integer, etc.)</li>
 *   <li>Numeric widening/narrowing (int → long, long → int, etc.)</li>
 *   <li>String → numeric and numeric → String</li>
 *   <li>Enum ↔ String</li>
 *   <li>Boolean ↔ String</li>
 * </ul>
 *
 * <p>For complex type pairs (e.g., ClassC → ClassB), use
 * {@code @TypeMorphField(deepMap = true)} and register a mapping in TypeMorph.
 */
public class DefaultTypeConverter implements TypeConverter {

    /** Map from primitive type to its wrapper. */
    private static final Map<Class<?>, Class<?>> PRIMITIVE_TO_WRAPPER = Map.of(
            int.class,     Integer.class,
            long.class,    Long.class,
            double.class,  Double.class,
            float.class,   Float.class,
            boolean.class, Boolean.class,
            short.class,   Short.class,
            byte.class,    Byte.class,
            char.class,    Character.class
    );

    /** Map from wrapper to its primitive. */
    private static final Map<Class<?>, Class<?>> WRAPPER_TO_PRIMITIVE = Map.of(
            Integer.class,   int.class,
            Long.class,      long.class,
            Double.class,    double.class,
            Float.class,     float.class,
            Boolean.class,   boolean.class,
            Short.class,     short.class,
            Byte.class,      byte.class,
            Character.class, char.class
    );

    private static final Set<Class<?>> NUMERIC_TYPES = Set.of(
            int.class, Integer.class, long.class, Long.class,
            double.class, Double.class, float.class, Float.class,
            short.class, Short.class, byte.class, Byte.class
    );

    @Override
    public boolean canConvert(Class<?> sourceType, Class<?> targetType) {
        if (sourceType == targetType)                            return true;
        if (targetType.isAssignableFrom(sourceType))            return true;
        if (isWrapperPair(sourceType, targetType))              return true;
        if (isNumericType(sourceType) && isNumericType(targetType)) return true;
        if (sourceType == String.class && isNumericType(targetType)) return true;
        if (isNumericType(sourceType) && targetType == String.class) return true;
        if (sourceType == String.class && targetType == Boolean.class) return true;
        if (sourceType == String.class && targetType == boolean.class) return true;
        if (sourceType == Boolean.class  && targetType == String.class) return true;
        if (sourceType == boolean.class  && targetType == String.class) return true;
        if (sourceType.isEnum() && targetType == String.class)  return true;
        if (sourceType == String.class && targetType.isEnum())  return true;
        return false;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public Object convert(Object value, Class<?> targetType) {
        if (value == null) return null;

        Class<?> sourceType = value.getClass();

        // Already assignable (covers same type and subtype)
        if (targetType.isInstance(value)) return value;

        // Primitive ↔ wrapper (auto-boxing/unboxing via reflection)
        if (isWrapperPair(sourceType, targetType)) return value; // JVM handles auto-box

        // String → numeric / boolean
        if (value instanceof String s) {
            if (targetType == Integer.class || targetType == int.class)   return Integer.parseInt(s.trim());
            if (targetType == Long.class    || targetType == long.class)   return Long.parseLong(s.trim());
            if (targetType == Double.class  || targetType == double.class) return Double.parseDouble(s.trim());
            if (targetType == Float.class   || targetType == float.class)  return Float.parseFloat(s.trim());
            if (targetType == Short.class   || targetType == short.class)  return Short.parseShort(s.trim());
            if (targetType == Byte.class    || targetType == byte.class)   return Byte.parseByte(s.trim());
            if (targetType == Boolean.class || targetType == boolean.class) return Boolean.parseBoolean(s.trim());
            if (targetType.isEnum()) return Enum.valueOf((Class<Enum>) targetType, s.trim());
        }

        // Numeric → String
        if (targetType == String.class) return String.valueOf(value);

        // Enum → String
        if (value instanceof Enum<?> e && targetType == String.class) return e.name();

        // Numeric widening / narrowing
        if (value instanceof Number n) {
            if (targetType == Long.class    || targetType == long.class)   return n.longValue();
            if (targetType == Integer.class || targetType == int.class)    return n.intValue();
            if (targetType == Double.class  || targetType == double.class) return n.doubleValue();
            if (targetType == Float.class   || targetType == float.class)  return n.floatValue();
            if (targetType == Short.class   || targetType == short.class)  return n.shortValue();
            if (targetType == Byte.class    || targetType == byte.class)   return n.byteValue();
        }

        throw new MorphFieldTypeException(sourceType, targetType);
    }

    // -------------------------------------------------------------------------

    private boolean isNumericType(Class<?> type) {
        return NUMERIC_TYPES.contains(type);
    }

    private boolean isWrapperPair(Class<?> a, Class<?> b) {
        return (PRIMITIVE_TO_WRAPPER.get(a) == b) || (PRIMITIVE_TO_WRAPPER.get(b) == a);
    }
}
