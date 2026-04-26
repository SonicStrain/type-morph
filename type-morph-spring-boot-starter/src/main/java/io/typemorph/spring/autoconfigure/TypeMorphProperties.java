package io.typemorph.spring.autoconfigure;

import io.typemorph.config.NullElementHandling;
import io.typemorph.config.NullHandling;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

/**
 * Configuration properties for type-morph, bindable from application.yml / application.properties.
 *
 * <pre>
 * typemorph:
 *   null-handling: RETURN_NULL          # default: THROW_EXCEPTION
 *   null-element-handling: SKIP         # default: THROW_EXCEPTION
 *   fail-on-missing-mapper: false       # default: true
 *   fail-on-ambiguous-mapper: false     # default: true
 *   scan-packages:                      # packages to scan for @TypeMorphClass
 *     - com.example.dto
 *     - com.example.entity
 *   enable-aop: true                    # register @TypeMorphAccepts AOP aspect (default: true)
 * </pre>
 */
@ConfigurationProperties(prefix = "typemorph")
public class TypeMorphProperties {

    private NullHandling nullHandling = NullHandling.THROW_EXCEPTION;
    private NullElementHandling nullElementHandling = NullElementHandling.THROW_EXCEPTION;
    private boolean failOnMissingMapper = true;
    private boolean failOnAmbiguousMapper = true;

    /**
     * Packages to scan for classes annotated with {@code @TypeMorphClass}.
     * Each entry is a base package name (e.g., {@code "com.example.dto"}).
     * All classes in the package and its sub-packages are inspected.
     */
    private List<String> scanPackages = new ArrayList<>();

    /**
     * When true (default), registers the {@link io.typemorph.spring.aspect.TypeMorphMethodAspect}
     * bean which powers {@link io.typemorph.spring.annotation.TypeMorphAccepts}.
     * Set to false to disable the AOP integration (reduces proxy overhead).
     */
    private boolean enableAop = true;

    public NullHandling getNullHandling() { return nullHandling; }
    public void setNullHandling(NullHandling nullHandling) { this.nullHandling = nullHandling; }

    public NullElementHandling getNullElementHandling() { return nullElementHandling; }
    public void setNullElementHandling(NullElementHandling h) { this.nullElementHandling = h; }

    public boolean isFailOnMissingMapper() { return failOnMissingMapper; }
    public void setFailOnMissingMapper(boolean fail) { this.failOnMissingMapper = fail; }

    public boolean isFailOnAmbiguousMapper() { return failOnAmbiguousMapper; }
    public void setFailOnAmbiguousMapper(boolean fail) { this.failOnAmbiguousMapper = fail; }

    public List<String> getScanPackages() { return scanPackages; }
    public void setScanPackages(List<String> scanPackages) { this.scanPackages = scanPackages; }

    public boolean isEnableAop() { return enableAop; }
    public void setEnableAop(boolean enableAop) { this.enableAop = enableAop; }
}
