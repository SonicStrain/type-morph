package io.typemorph.exception;

/**
 * Thrown at registration/startup time when annotation configuration is invalid.
 * Examples:
 * <ul>
 *   <li>Duplicate {@code @TypeMorphField} for the same target on the same source field</li>
 *   <li>{@code NullFieldBehavior.SET_NULL} on a primitive target field</li>
 *   <li>A final non-record target field that cannot be written</li>
 *   <li>A field declared in {@code @TypeMorphField(name = ...)} that does not exist in the target</li>
 *   <li>An inaccessible field that cannot be opened via reflection</li>
 * </ul>
 *
 * <p>This is a fail-fast exception — it is thrown during context startup, not during mapping.
 */
public class MorphConfigurationException extends MorphException {

    public MorphConfigurationException(String message) {
        super(message);
    }

    public MorphConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }
}
