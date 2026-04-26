package io.typemorph.spring;

import io.typemorph.TypeMorph;
import io.typemorph.annotation.TypeMorphClass;
import io.typemorph.spring.annotation.TypeMorphAccepts;
import io.typemorph.spring.aspect.TypeMorphMethodAspect;
import io.typemorph.spring.autoconfigure.TypeMorphAutoConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.aop.AopAutoConfiguration;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;

import static org.assertj.core.api.Assertions.*;

/**
 * Integration tests for the {@link TypeMorphMethodAspect} AOP integration.
 *
 * <p>Tests use a return-value pattern (instead of field mutation) to avoid the
 * CGLIB proxy/target object split: with CGLIB proxies, the proxy and the wrapped
 * target are separate object instances. State mutations inside a proxied method
 * happen on the target, not on the proxy reference. Return values, however, flow
 * back through the proxy call chain and are always visible to the caller.
 */
class TypeMorphAopTest {

    // -------------------------------------------------------------------------
    // Fixture classes
    // -------------------------------------------------------------------------

    @TypeMorphClass(targets = OrderDto.class)
    static class OrderEntity {
        String orderId;
        double amount;
        OrderEntity(String id, double amt) { orderId = id; amount = amt; }
    }

    static class OrderDto {
        String orderId;
        double amount;
    }

    /**
     * Service whose {@code process} method accepts any registered source type and
     * returns the converted {@link OrderDto}. Return values pass cleanly through
     * the AOP proxy so callers can always inspect them.
     */
    @Service
    static class OrderService {
        /**
         * Accepts either an {@link OrderDto} directly or a registered source type
         * (e.g., {@link OrderEntity}). The AOP aspect converts the argument before
         * the method body runs.
         *
         * @return the (possibly converted) OrderDto
         */
        public OrderDto process(@TypeMorphAccepts(OrderDto.class) Object order) {
            return (OrderDto) order; // safe: AOP guarantees the cast succeeds
        }
    }

    @Configuration
    static class TestConfig {
        @Bean
        OrderService orderService() { return new OrderService(); }

        @Bean
        TypeMorph typeMorph() {
            return new TypeMorph().scan(OrderEntity.class);
        }
    }

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withConfiguration(AutoConfigurations.of(
                    TypeMorphAutoConfiguration.class,
                    AopAutoConfiguration.class))          // enables @Aspect processing
            .withUserConfiguration(TestConfig.class);

    // -------------------------------------------------------------------------
    // Tests
    // -------------------------------------------------------------------------

    @Test
    void aspectBeanIsRegistered() {
        contextRunner.run(ctx ->
            assertThat(ctx).hasSingleBean(TypeMorphMethodAspect.class)
        );
    }

    @Test
    void aspectConvertsEntityToDtoTransparently() {
        contextRunner.run(ctx -> {
            OrderService service = ctx.getBean(OrderService.class);
            OrderEntity entity = new OrderEntity("ORD-42", 199.99);

            OrderDto result = service.process(entity); // passes OrderEntity, returns OrderDto

            assertThat(result).isNotNull();
            assertThat(result.orderId).isEqualTo("ORD-42");
            assertThat(result.amount).isCloseTo(199.99, within(0.001));
        });
    }

    @Test
    void aspectPassesThroughAlreadyCorrectType() {
        contextRunner.run(ctx -> {
            OrderService service = ctx.getBean(OrderService.class);
            OrderDto dto = new OrderDto();
            dto.orderId = "ORD-99";
            dto.amount  = 50.0;

            OrderDto result = service.process(dto); // already OrderDto — pass through

            assertThat(result.orderId).isEqualTo("ORD-99");
        });
    }

    @Test
    void aspectDisabled_whenEnableAopFalse() {
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(
                        TypeMorphAutoConfiguration.class,
                        AopAutoConfiguration.class))
                .withPropertyValues("typemorph.enable-aop=false")
                .run(ctx ->
                    assertThat(ctx).doesNotHaveBean(TypeMorphMethodAspect.class)
                );
    }

    @Test
    void scanPackages_registersAnnotatedClasses() {
        // Verify that typemorph.scan-packages causes @TypeMorphClass classes to be registered.
        // Scan the package where OrderEntity is declared (this test class's package).
        new ApplicationContextRunner()
                .withConfiguration(AutoConfigurations.of(TypeMorphAutoConfiguration.class))
                .withPropertyValues(
                    "typemorph.scan-packages[0]=io.typemorph.spring",
                    "typemorph.fail-on-missing-mapper=false",
                    "typemorph.fail-on-ambiguous-mapper=false")
                .run(ctx -> {
                    TypeMorph morph = ctx.getBean(TypeMorph.class);
                    assertThat(morph.canMap(OrderEntity.class, OrderDto.class)).isTrue();
                });
    }
}
