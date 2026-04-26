package io.typemorph.spring.aspect;

import io.typemorph.TypeMorph;
import io.typemorph.spring.annotation.TypeMorphAccepts;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;

/**
 * Spring AOP aspect that intercepts method calls and automatically converts arguments
 * annotated with {@link TypeMorphAccepts} to the declared target type.
 *
 * <h3>How it works</h3>
 * <ol>
 *   <li>Intercepts every Spring bean method call (around advice on all public methods).</li>
 *   <li>Inspects method parameters for {@link TypeMorphAccepts} annotations.</li>
 *   <li>For each annotated parameter, calls
 *       {@link TypeMorph#ensureType(Object, Class)} on the argument.</li>
 *   <li>If the argument is already the correct type, it is passed through unchanged.</li>
 *   <li>Proceeds with the (possibly converted) argument array.</li>
 * </ol>
 *
 * <h3>Performance</h3>
 * The aspect is only active when {@link TypeMorphAccepts} is present on a parameter.
 * Methods without the annotation incur only a parameter inspection cost (fast reflection
 * on pre-cached method metadata). The annotation check exits early when no parameters
 * are annotated.
 */
@Aspect
public class TypeMorphMethodAspect {

    private final TypeMorph typeMorph;

    public TypeMorphMethodAspect(TypeMorph typeMorph) {
        this.typeMorph = typeMorph;
    }

    /**
     * Around advice that applies {@link TypeMorphAccepts} conversions before proceeding.
     * Intercepts all public methods on Spring-managed beans.
     */
    @Around("execution(public * *(.., @io.typemorph.spring.annotation.TypeMorphAccepts (*), ..))")
    public Object convertAnnotatedParameters(ProceedingJoinPoint pjp) throws Throwable {
        MethodSignature sig    = (MethodSignature) pjp.getSignature();
        Method          method = sig.getMethod();
        Parameter[]     params = method.getParameters();
        Object[]        args   = pjp.getArgs().clone(); // clone so we don't mutate the original

        boolean anyConverted = false;
        for (int i = 0; i < params.length; i++) {
            TypeMorphAccepts ann = params[i].getAnnotation(TypeMorphAccepts.class);
            if (ann == null) continue;

            @SuppressWarnings("unchecked")
            Class<Object> targetType = (Class<Object>) ann.value();
            Object converted = typeMorph.ensureType(args[i], targetType);
            if (converted != args[i]) {
                args[i]      = converted;
                anyConverted = true;
            }
        }

        return anyConverted ? pjp.proceed(args) : pjp.proceed();
    }
}
