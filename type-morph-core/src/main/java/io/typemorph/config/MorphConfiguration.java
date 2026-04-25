package io.typemorph.config;

import java.util.Objects;

/**
 * Immutable configuration for a TypeMorph instance. Build via MorphConfiguration.builder().
 */
public final class MorphConfiguration {

    private final NullHandling nullHandling;
    private final NullElementHandling nullElementHandling;
    private final boolean failOnMissingMapper;
    private final boolean failOnAmbiguousMapper;

    private MorphConfiguration(Builder builder) {
        this.nullHandling = builder.nullHandling;
        this.nullElementHandling = builder.nullElementHandling;
        this.failOnMissingMapper = builder.failOnMissingMapper;
        this.failOnAmbiguousMapper = builder.failOnAmbiguousMapper;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static MorphConfiguration defaults() {
        return builder().build();
    }

    public NullHandling getNullHandling() {
        return nullHandling;
    }

    public NullElementHandling getNullElementHandling() {
        return nullElementHandling;
    }

    public boolean isFailOnMissingMapper() {
        return failOnMissingMapper;
    }

    public boolean isFailOnAmbiguousMapper() {
        return failOnAmbiguousMapper;
    }

    public static final class Builder {
        private NullHandling nullHandling = NullHandling.THROW_EXCEPTION;
        private NullElementHandling nullElementHandling = NullElementHandling.THROW_EXCEPTION;
        private boolean failOnMissingMapper = true;
        private boolean failOnAmbiguousMapper = true;

        private Builder() {}

        public Builder nullHandling(NullHandling nullHandling) {
            this.nullHandling = Objects.requireNonNull(nullHandling, "nullHandling must not be null");
            return this;
        }

        public Builder nullElementHandling(NullElementHandling nullElementHandling) {
            this.nullElementHandling = Objects.requireNonNull(nullElementHandling, "nullElementHandling must not be null");
            return this;
        }

        public Builder failOnMissingMapper(boolean fail) {
            this.failOnMissingMapper = fail;
            return this;
        }

        public Builder failOnAmbiguousMapper(boolean fail) {
            this.failOnAmbiguousMapper = fail;
            return this;
        }

        public MorphConfiguration build() {
            return new MorphConfiguration(this);
        }
    }
}
