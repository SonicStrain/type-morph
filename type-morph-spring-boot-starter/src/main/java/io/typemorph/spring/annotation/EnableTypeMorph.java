package io.typemorph.spring.annotation;

import io.typemorph.spring.autoconfigure.TypeMorphAutoConfiguration;
import org.springframework.context.annotation.Import;

import java.lang.annotation.*;

/**
 * Explicitly enables type-morph configuration.
 *
 * <p>Not required when using Spring Boot's auto-configuration mechanism
 * (i.e., when the starter is on the classpath). Useful for:
 * <ul>
 *   <li>Non-Boot Spring applications that still want type-morph</li>
 *   <li>Test configurations that need explicit import control</li>
 * </ul>
 *
 * <pre>
 *   {@literal @}SpringBootApplication
 *   {@literal @}EnableTypeMorph
 *   public class MyApp { ... }
 * </pre>
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Import(TypeMorphAutoConfiguration.class)
public @interface EnableTypeMorph {
}
