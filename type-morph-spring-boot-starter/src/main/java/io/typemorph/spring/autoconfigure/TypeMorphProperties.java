package io.typemorph.spring.autoconfigure;

import io.typemorph.config.NullElementHandling;
import io.typemorph.config.NullHandling;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuration properties for type-morph, bindable from application.yml / application.properties.
 *
 * <pre>
 * typemorph:
 *   null-handling: RETURN_NULL          # default: THROW_EXCEPTION
 *   null-element-handling: SKIP         # default: THROW_EXCEPTION
 *   fail-on-missing-mapper: false       # default: true
 *   fail-on-ambiguous-mapper: false     # default: true
 * </pre>
 */
@ConfigurationProperties(prefix = "typemorph")
public class TypeMorphProperties {

    private NullHandling nullHandling = NullHandling.THROW_EXCEPTION;
    private NullElementHandling nullElementHandling = NullElementHandling.THROW_EXCEPTION;
    private boolean failOnMissingMapper = true;
    private boolean failOnAmbiguousMapper = true;

    public NullHandling getNullHandling() { return nullHandling; }
    public void setNullHandling(NullHandling nullHandling) { this.nullHandling = nullHandling; }

    public NullElementHandling getNullElementHandling() { return nullElementHandling; }
    public void setNullElementHandling(NullElementHandling h) { this.nullElementHandling = h; }

    public boolean isFailOnMissingMapper() { return failOnMissingMapper; }
    public void setFailOnMissingMapper(boolean fail) { this.failOnMissingMapper = fail; }

    public boolean isFailOnAmbiguousMapper() { return failOnAmbiguousMapper; }
    public void setFailOnAmbiguousMapper(boolean fail) { this.failOnAmbiguousMapper = fail; }
}
