package io.typemorph.spring.annotation;

import java.lang.annotation.*;

/**
 * Declares that a method parameter of type {@code Object} (or a supertype) should be
 * automatically converted to the specified {@code value} type before the method executes.
 *
 * <p>When placed on a method parameter, the Spring AOP aspect
 * {@link io.typemorph.spring.aspect.TypeMorphMethodAspect} intercepts the call and uses
 * {@link io.typemorph.TypeMorph#ensureType(Object, Class)} to transparently convert the
 * argument if it is not already an instance of {@code value}.
 *
 * <h3>Use case</h3>
 * This solves the compile-time constraint that Java's type system cannot allow
 * {@code methodName(classC)} when the signature is {@code methodName(ClassB)}.
 * By changing the parameter type to {@code Object} and annotating with
 * {@code @TypeMorphAccepts(ClassB.class)}, callers can pass any registered source type:
 *
 * <pre>
 * // Signature
 * public void processOrder(@TypeMorphAccepts(OrderDto.class) Object order) {
 *     OrderDto dto = (OrderDto) order; // safe — AOP guarantees this cast
 *     ...
 * }
 *
 * // Callers can pass either an OrderDto or an OrderEntity (if mapped):
 * service.processOrder(orderDto);      // passed through unchanged
 * service.processOrder(orderEntity);   // converted to OrderDto transparently
 * </pre>
 *
 * <p><strong>Requirements:</strong>
 * <ul>
 *   <li>The target class must be registered in the {@link io.typemorph.TypeMorph} bean.</li>
 *   <li>The Spring bean must be a Spring-managed proxy (called via Spring context).</li>
 *   <li>The annotated method must be on a Spring-managed bean.</li>
 * </ul>
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface TypeMorphAccepts {

    /**
     * The target type the parameter should be converted to.
     * TypeMorph will call {@code ensureType(argument, value())} before the method runs.
     */
    Class<?> value();
}
