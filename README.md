<div align="center">

# 🔄 type-morph

### Zero-boilerplate class mapping for Java

*Polymorphic method support &nbsp;·&nbsp; Annotation-driven field binding &nbsp;·&nbsp; Spring Boot ready*

[![CI](https://github.com/SonicStrain/type-morph/actions/workflows/ci.yml/badge.svg?branch=release)](https://github.com/SonicStrain/type-morph/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/Java-17%2B-ED8B00?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.x-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.sonicstrain/type-morph-core?label=Maven%20Central&logo=apachemaven&logoColor=white)](https://central.sonatype.com/artifact/io.github.sonicstrain/type-morph-core)

</div>

---

`type-morph` is a lightweight Java library that eliminates the repetitive code required to map between two different classes (e.g., Entity ↔ DTO). It provides two complementary approaches: **lambda-based mapping** for full manual control and **annotation-driven reflective mapping** for automatic field-level binding — with a shared `map()` call that works across all registered type pairs.

---

## Table of Contents

- [The Problem](#the-problem)
- [The Solution](#the-solution)
- [Features](#features)
- [Requirements](#requirements)
- [Installation](#installation)
- [Mapping Approach 1 — Lambda / Named Class](#mapping-approach-1--lambda--named-class)
- [Mapping Approach 2 — Annotation-Driven (Reflective)](#mapping-approach-2--annotation-driven-reflective)
  - [@TypeMorphClass](#typemorphclass)
  - [@TypeMorphField — Rename, deepMap, NullFieldBehavior](#typemorphfield--rename-deepmap-nullfieldbehavior)
  - [@TypeMorphIgnore](#typemorphignore)
  - [@TypeMorphConstructor](#typemorphconstructor)
  - [Java Record Support](#java-record-support)
  - [Automatic Type Conversion](#automatic-type-conversion)
  - [Deep / Nested Mapping](#deep--nested-mapping)
  - [Bidirectional Annotation Mapping](#bidirectional-annotation-mapping)
- [Polymorphic Method Pattern](#polymorphic-method-pattern)
- [ensureType — Transparent Type Coercion](#ensuretype--transparent-type-coercion)
- [Spring Boot Integration](#spring-boot-integration)
  - [Auto-Registration of MorphMapping Beans](#auto-registration-of-morphmapping-beans)
  - [Package Scanning for @TypeMorphClass](#package-scanning-for-typemorphclass)
  - [@TypeMorphAccepts — AOP Method Parameter Conversion](#typemorphaccepts--aop-method-parameter-conversion)
  - [application.yml Configuration](#applicationyml-configuration)
- [Bidirectional Mapping (Lambda)](#bidirectional-mapping-lambda)
- [Collection Mapping](#collection-mapping)
- [Configuration Reference](#configuration-reference)
- [Error Handling](#error-handling)
- [Project Structure](#project-structure)
- [Design Notes](#design-notes)
- [Building from Source](#building-from-source)

---

## The Problem

### Problem 1 — Boilerplate mapping code

Every project ends up with code like this, repeated dozens of times:

```java
public UserDto toDto(UserEntity entity) {
    UserDto dto = new UserDto();
    dto.setId(entity.getId());
    dto.setFullName(entity.getFirstName() + " " + entity.getLastName());
    dto.setEmail(entity.getEmail());
    return dto;
}

public OrderDto toDto(OrderEntity entity) {
    OrderDto dto = new OrderDto();
    dto.setOrderId(entity.getId());
    dto.setDesc(entity.getDescription());
    return dto;
}
```

### Problem 2 — Repeated method overloads for the same logic

```java
public ClassA processAndReturn(ClassB input) { /* same logic */ }
public ClassC processAndReturn(ClassD input) { /* exact same logic, different types */ }
public ClassE processAndReturn(ClassF input) { /* exact same logic, different types again */ }
```

### Problem 3 — Compile-time wall at method boundaries

When an existing method expects `ClassB` but the caller has `ClassC`, Java's type system forces an overload or an explicit conversion call at every call site — even when a registered mapping exists.

---

## The Solution

`type-morph` provides a central registry. Register your mappings once — either as lambdas or via annotations on your classes — then use a single `map()` call that resolves the right conversion at runtime.

```java
// Annotation-driven: zero boilerplate — just annotate your source class
@TypeMorphClass(targets = UserDto.class)
public class UserEntity {
    @TypeMorphField(name = "fullName")
    private String name;
    private String email;
    // ...
}

// Register and use
TypeMorph morph = new TypeMorph().scan(UserEntity.class, OrderEntity.class);

UserDto  user  = morph.map(userEntity);   // → UserDto
OrderDto order = morph.map(orderEntity);  // → OrderDto (same method!)
```

---

## Features

- **Two registration styles** — explicit lambda/class mapping OR annotation-driven reflective binding
- **Polymorphic `map()`** — same method call works for all registered type pairs; return type inferred at call site
- **`@TypeMorphClass`** — mark any class to auto-map to one or more target types by field name
- **`@TypeMorphField`** — rename fields, enable deep/nested mapping, control null behavior per field
- **`@TypeMorphIgnore`** — exclude fields from mapping, globally or per-target
- **`@TypeMorphConstructor`** — use a specific constructor when no-arg isn't available
- **Java Record support** — records as both source and target; canonical constructor used automatically
- **Automatic type conversion** — int↔long, numeric widening/narrowing, String↔numeric, Enum↔String, Boolean↔String
- **Deep / nested mapping** — `@TypeMorphField(deepMap = true)` recursively maps nested objects
- **`NullFieldBehavior`** — per-field: `SKIP` (default), `SET_NULL`, or `THROW`
- **Cycle detection** — circular references are caught early and reported with actionable messages
- **Fail-fast validation** — configuration errors surface at startup (scan time), not during the first `map()` call
- **Bidirectional mapping** — define A→B once, get B→A automatically with `bidirectional = true`
- **`ensureType()`** — transparently converts an object to a target type if it isn't already one
- **`@TypeMorphAccepts`** (Spring) — AOP annotation that auto-converts method parameters
- **`scan-packages`** (Spring) — configure packages to scan in `application.yml`
- **Collection mapping** — `mapList()` converts entire lists
- **Safe mapping** — `mapSafe()` returns `Optional` instead of throwing
- **Thread-safe registry** — concurrent reads, synchronized writes at startup
- **Zero mandatory dependencies** — core module has no runtime dependencies

---

## Requirements

- Java 17+
- Maven 3.6+ or Gradle 7+
- Spring Boot 3.x *(only for the starter module — optional)*

---

## Installation

### Maven

**Core only (no Spring):**
```xml
<dependency>
    <groupId>io.github.sonicstrain</groupId>
    <artifactId>type-morph-core</artifactId>
    <version>1.0.0</version>
</dependency>
```

**Spring Boot starter (includes core):**
```xml
<dependency>
    <groupId>io.github.sonicstrain</groupId>
    <artifactId>type-morph-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

### Gradle

**Core only:**
```groovy
implementation 'io.github.sonicstrain:type-morph-core:1.0.0'
```

**Spring Boot starter:**
```groovy
implementation 'io.github.sonicstrain:type-morph-spring-boot-starter:1.0.0'
```

---

## Mapping Approach 1 — Lambda / Named Class

Register mappings programmatically as lambdas or named classes. Full control over every line of mapping logic.

```java
TypeMorph morph = new TypeMorph()
    // Lambda — explicit types required (Java erases lambda generics)
    .register(UserEntity.class, UserDto.class,
        entity -> new UserDto(entity.getId(), entity.getFirstName() + " " + entity.getLastName()))

    // Named class — types resolved automatically via reflection
    .register(new OrderEntityMapper());   // implements MorphMapping<OrderEntity, OrderDto>

// Same map() call, different type pairs
UserDto  userDto  = morph.map(userEntity);
OrderDto orderDto = morph.map(orderEntity);
```

> **Why can't lambdas auto-register?**
> Java erases generic type arguments for lambdas at compile time. Use `register(SourceClass, TargetClass, lambda)` for lambdas, or implement a named class and call `register(namedInstance)`.

---

## Mapping Approach 2 — Annotation-Driven (Reflective)

Annotate your source class. Call `morph.scan()`. TypeMorph inspects all non-static fields and generates the mapping automatically — including inherited fields, type conversions, and record support.

All validation happens at **scan time** (startup): incompatible field types, final target fields, missing constructors — everything surfaces immediately, not during the first `map()` call.

### `@TypeMorphClass`

```java
@TypeMorphClass(targets = UserDto.class)
public class UserEntity {
    private Long   id;
    private String name;
    private String email;
    // getters/setters or public fields — setAccessible is called automatically
}

public class UserDto {
    private Long   id;
    private String name;
    private String email;
}

// Register via scan
TypeMorph morph = new TypeMorph().scan(UserEntity.class);
UserDto dto = morph.map(userEntity, UserDto.class);
```

Fields are matched by **name**. Fields that exist in the source but not in the target are silently skipped. Fields in the target with no source counterpart keep their default values.

**Multiple targets:**
```java
@TypeMorphClass(targets = { UserDto.class, UserSummary.class })
public class UserEntity {
    private Long   id;
    private String name;
    private String email;
}
```

---

### `@TypeMorphField` — Rename, deepMap, NullFieldBehavior

`@TypeMorphField` customizes how a single source field is mapped. It is **repeatable** — you can apply different settings per target.

```java
@TypeMorphClass(targets = { OrderDto.class, OrderSummary.class })
public class OrderEntity {

    // Different target field name per target
    @TypeMorphField(target = OrderDto.class,     name = "orderId")
    @TypeMorphField(target = OrderSummary.class, name = "id")
    private String referenceCode;

    // Wildcard: applies to ALL targets (both OrderDto and OrderSummary)
    @TypeMorphField(name = "totalAmount")
    private BigDecimal amount;

    // Null behavior: actively write null to target (default is SKIP)
    @TypeMorphField(onNull = NullFieldBehavior.SET_NULL)
    private String notes;

    // Deep / nested mapping — see section below
    @TypeMorphField(deepMap = true)
    private AddressEntity address;
}
```

**`@TypeMorphField` attributes:**

| Attribute | Type | Default | Description |
|---|---|---|---|
| `target` | `Class<?>` | `Void.class` | Which target this annotation applies to. `Void.class` = all targets (wildcard). |
| `name` | `String` | `""` (empty) | Target field name. Empty = same name as source field. |
| `deepMap` | `boolean` | `false` | If `true`, calls `typeMorph.map(value)` recursively for nested object conversion. |
| `onNull` | `NullFieldBehavior` | `SKIP` | What to do when the source field value is `null`. |

**`NullFieldBehavior` options:**

| Value | Behavior |
|---|---|
| `SKIP` | *(default)* Leave the target field at its initialized value. |
| `SET_NULL` | Actively write `null` to the target field. Cannot be used with primitive target fields. |
| `THROW` | Throw `MorphFieldMappingException` immediately. |

---

### `@TypeMorphIgnore`

Exclude a source field from mapping entirely. Also repeatable and target-aware.

```java
@TypeMorphClass(targets = { PublicDto.class, InternalDto.class })
public class UserEntity {

    private String username;

    // Ignored only when mapping to PublicDto — still mapped to InternalDto
    @TypeMorphIgnore(target = PublicDto.class)
    private String passwordHash;

    // Ignored for ALL targets
    @TypeMorphIgnore
    private transient String sessionToken;
}
```

---

### `@TypeMorphConstructor`

If the target class has no no-arg constructor, annotate the constructor you want TypeMorph to use. TypeMorph calls it with `null`/default arguments to create the instance, then sets fields individually via reflection.

```java
public class AddressDto {
    private String street;
    private String city;

    @TypeMorphConstructor      // TypeMorph will call this with (null, null) then set fields
    public AddressDto(String street, String city) {
        this.street = street;
        this.city   = city;
    }
}
```

---

### Java Record Support

Records work as both **source** and **target**. When mapping **to** a record, TypeMorph uses the canonical constructor — no boilerplate, no workarounds needed.

```java
// Record as target
record PersonDto(String name, int age) {}

@TypeMorphClass(targets = PersonDto.class)
public class PersonEntity {
    private String name;
    private int    age;
    // ... no-arg constructor for source is fine
}

TypeMorph morph = new TypeMorph().scan(PersonEntity.class);
PersonDto dto = morph.map(entity, PersonDto.class);
// dto.name() and dto.age() are set via the canonical constructor

// Record as source
@TypeMorphClass(targets = PersonEntity.class)
record PersonRecord(String name, int age) {}

TypeMorph morph = new TypeMorph().scan(PersonRecord.class);
PersonEntity entity = morph.map(record, PersonEntity.class);
```

Unmatched record components default to `null` (or primitive zero). Unused source fields are silently skipped.

---

### Automatic Type Conversion

When source and target field types differ, TypeMorph automatically converts values using a built-in set of conversions — no configuration required:

| Conversion | Example |
|---|---|
| Primitive ↔ Wrapper | `int` ↔ `Integer` |
| Numeric widening | `int` → `long`, `int` → `double` |
| Numeric narrowing | `long` → `int`, `double` → `float` |
| Number → String | `42` → `"42"` |
| String → Number | `"42"` → `42` |
| Enum → String | `Color.RED` → `"RED"` |
| String → Enum | `"RED"` → `Color.RED` |
| Boolean → String | `true` → `"true"` |
| String → Boolean | `"true"` → `true` |

```java
@TypeMorphClass(targets = ProductDto.class)
public class ProductEntity {
    private int    quantity;   // int → long in ProductDto
    private long   price;      // long → String in ProductDto
    private Color  status;     // Enum → String in ProductDto
}

public class ProductDto {
    private long   quantity;
    private String price;
    private String status;
}
```

If the field types are incompatible and no built-in conversion exists, a `MorphConfigurationException` is thrown **at scan time** with a clear message pointing to `@TypeMorphField(deepMap = true)` as the solution for complex type pairs.

---

### Deep / Nested Mapping

Use `deepMap = true` to recursively convert a nested object through TypeMorph. The nested type must itself have a registered mapping.

```java
@TypeMorphClass(targets = AddressDto.class)
public class AddressEntity {
    private String street;
    private String city;
}

@TypeMorphClass(targets = OrderDto.class)
public class OrderEntity {
    private String        orderId;

    @TypeMorphField(deepMap = true)
    private AddressEntity address;   // AddressEntity → AddressDto, recursively
}

public class OrderDto {
    private String     orderId;
    private AddressDto address;
}

TypeMorph morph = new TypeMorph().scan(AddressEntity.class, OrderEntity.class);
OrderDto dto = morph.map(orderEntity, OrderDto.class);
// dto.address is an AddressDto, not an AddressEntity
```

**Circular reference protection:** TypeMorph tracks all in-progress objects per thread using an `IdentityHashMap`. If the same object is encountered again mid-mapping, a `MorphCircularReferenceException` is thrown. Break cycles with `@TypeMorphIgnore` on one side.

```java
@TypeMorphClass(targets = NodeDto.class)
public class NodeEntity {
    private String     label;

    @TypeMorphIgnore   // breaks the circular reference
    private NodeEntity parent;
}
```

---

### Bidirectional Annotation Mapping

Add `bidirectional = true` to automatically register the reverse mapping (target → source) as well. No annotations are needed on the target class for the reverse direction.

```java
@TypeMorphClass(targets = UserDto.class, bidirectional = true)
public class UserEntity {
    private Long   id;
    private String name;
}

public class UserDto {
    private Long   id;
    private String name;
    // Must have a no-arg constructor (or @TypeMorphConstructor) for reverse mapping
}

TypeMorph morph = new TypeMorph().scan(UserEntity.class);

UserDto    dto    = morph.map(entity, UserDto.class);    // forward
UserEntity entity = morph.map(dto, UserEntity.class);    // reverse — auto-registered
```

---

## Polymorphic Method Pattern

This is the core differentiator of `type-morph`. A single `map()` call handles all registered type pairs. The return type `T` is inferred by the JVM from the call-site assignment:

```java
// Both annotation-driven and lambda mappings use the same call
UserDto  user  = morph.map(userEntity);    // registry returns UserDto
OrderDto order = morph.map(orderEntity);   // registry returns OrderDto — same method!

// When a source type maps to multiple targets, disambiguate with an explicit class
UserDto     full    = morph.map(userEntity, UserDto.class);
UserSummary summary = morph.map(userEntity, UserSummary.class);
```

**In a service:**
```java
@Service
public class EntityService {
    @Autowired
    private TypeMorph morph;

    public <T> T fetchAndConvert(Long id, Class<?> entityType, Class<T> targetType) {
        Object entity = repository.findById(entityType, id);
        return morph.map(entity, targetType);
    }
}

UserDto    user    = service.fetchAndConvert(1L, UserEntity.class,    UserDto.class);
OrderDto   order   = service.fetchAndConvert(2L, OrderEntity.class,   OrderDto.class);
ProductDto product = service.fetchAndConvert(3L, ProductEntity.class, ProductDto.class);
```

---

## `ensureType` — Transparent Type Coercion

`ensureType(input, TargetType.class)` returns `input` unchanged if it's already the target type, and converts it via `map()` otherwise. Use this at method boundaries to accept any registered source type without changing the public API:

```java
public class OrderService {

    public void processOrder(Object input) {
        // Accepts OrderDto directly OR anything that maps to OrderDto (e.g., OrderEntity)
        OrderDto dto = morph.ensureType(input, OrderDto.class);
        // dto is guaranteed to be OrderDto regardless of what input was
        ...
    }
}

// Callers can pass either type:
service.processOrder(orderDto);      // passed through — no conversion
service.processOrder(orderEntity);   // converted to OrderDto transparently
```

---

## Spring Boot Integration

Add the starter and everything wires up automatically. No `@Bean` definitions required unless you need to customize behavior.

```xml
<dependency>
    <groupId>io.github.sonicstrain</groupId>
    <artifactId>type-morph-spring-boot-starter</artifactId>
    <version>1.0.0</version>
</dependency>
```

Then inject `TypeMorph` anywhere:

```java
@Service
public class MyService {
    @Autowired
    private TypeMorph morph;

    public UserDto getUser(Long id) {
        return morph.map(userRepository.findById(id).orElseThrow(), UserDto.class);
    }
}
```

---

### Auto-Registration of MorphMapping Beans

Any Spring bean that implements `MorphMapping<S, T>` is discovered automatically and registered before your application starts:

```java
@Component
public class UserMapper implements MorphMapping<UserEntity, UserDto> {
    @Override
    public UserDto map(UserEntity source) {
        return new UserDto(source.getId(), source.getName());
    }
}
```

Spring's `ResolvableType` is used to resolve `S` and `T`, so CGLIB-proxied beans (e.g., `@Transactional` mappers) resolve correctly.

---

### Package Scanning for `@TypeMorphClass`

To automatically register all `@TypeMorphClass`-annotated classes in a package, configure `scan-packages` in `application.yml`:

```yaml
typemorph:
  scan-packages:
    - com.example.entity
    - com.example.model
```

All classes in those packages (and sub-packages) annotated with `@TypeMorphClass` are scanned and registered on startup. Configuration errors surface immediately as `MorphConfigurationException`.

---

### `@TypeMorphAccepts` — AOP Method Parameter Conversion

`@TypeMorphAccepts` solves the compile-time wall at method boundaries. Java's type system prevents passing `ClassC` to a method that expects `ClassB`. Change the parameter type to `Object`, annotate with `@TypeMorphAccepts`, and TypeMorph's AOP aspect converts the argument before the method body runs — transparently:

```java
@Service
public class OrderService {

    // Before: only accepts OrderDto — callers with OrderEntity must convert manually
    // public void process(OrderDto order) { ... }

    // After: accepts any registered source type, auto-converted to OrderDto
    public OrderDto process(@TypeMorphAccepts(OrderDto.class) Object order) {
        OrderDto dto = (OrderDto) order;  // AOP guarantees this cast is safe
        return dto;
    }
}
```

```java
// Callers:
service.process(orderDto);      // already OrderDto — passed through unchanged
service.process(orderEntity);   // OrderEntity → converted to OrderDto by the AOP aspect
```

**How it works:**
1. Spring AOP intercepts the method call before it reaches the body.
2. The `TypeMorphMethodAspect` inspects each parameter for `@TypeMorphAccepts`.
3. It calls `morph.ensureType(arg, annotatedType)` on each annotated parameter.
4. If the argument is already the correct type, it is passed through as-is.
5. The method body receives the guaranteed-correct type.

**Enable / disable:**
```yaml
typemorph:
  enable-aop: false   # disables TypeMorphMethodAspect (default: true)
```

**Requirements:**
- The method must be on a Spring-managed bean called through the Spring context (proxy).
- The target type must have a registered mapping in the `TypeMorph` bean.

---

### `application.yml` Configuration

```yaml
typemorph:
  null-handling: RETURN_NULL          # RETURN_NULL | THROW_EXCEPTION (default)
  null-element-handling: SKIP         # SKIP | THROW_EXCEPTION (default)
  fail-on-missing-mapper: true        # default: true
  fail-on-ambiguous-mapper: true      # default: true
  enable-aop: true                    # enable @TypeMorphAccepts AOP (default: true)
  scan-packages:                      # packages to scan for @TypeMorphClass
    - com.example.entity
    - com.example.dto
```

#### Override the TypeMorph bean

Define your own `@Bean` — auto-configuration backs off:

```java
@Configuration
public class MyMorphConfig {

    @Bean
    public TypeMorph typeMorph() {
        return new TypeMorph(
                MorphConfiguration.builder()
                        .nullHandling(NullHandling.RETURN_NULL)
                        .build())
            .scan(UserEntity.class, OrderEntity.class)
            .register(ProductEntity.class, ProductDto.class,
                      p -> new ProductDto(p.getSku(), p.getPrice()));
    }
}
```

#### Non-Boot Spring applications

Use `@EnableTypeMorph` to import the configuration explicitly:

```java
@Configuration
@EnableTypeMorph
public class AppConfig { }
```

---

## Bidirectional Mapping (Lambda)

For full control over both directions, use `BidirectionalMorphMapping`:

```java
BidirectionalMorphMapping<UserEntity, UserDto> biMapper = new BidirectionalMorphMapping<>() {
    @Override
    public UserDto forward(UserEntity source) {
        return new UserDto(source.getId(), source.getFullName());
    }

    @Override
    public UserEntity reverse(UserDto source) {
        String[] parts = source.getFullName().split(" ", 2);
        return new UserEntity(source.getId(), parts[0], parts.length > 1 ? parts[1] : "");
    }
};

TypeMorph morph = new TypeMorph()
    .register(UserEntity.class, UserDto.class,    biMapper.forwardMapping())
    .register(UserDto.class,    UserEntity.class, biMapper.reverseMapping());

UserDto    dto    = morph.map(entity);   // Entity → DTO
UserEntity entity = morph.map(dto);      // DTO → Entity
```

---

## Collection Mapping

```java
List<UserEntity> entities = userRepository.findAll();

// Inferred element type
List<UserDto> dtos = morph.mapList(entities);

// Explicit element type
List<UserDto> dtos = morph.mapList(entities, UserDto.class);

// Null element handling
TypeMorph morph = new TypeMorph(
    MorphConfiguration.builder()
        .nullElementHandling(NullElementHandling.SKIP)  // skip nulls silently
        .build()
).scan(UserEntity.class);

List<UserDto> dtos = morph.mapList(entitiesWithNulls);  // nulls skipped
```

---

## Configuration Reference

### Programmatic

```java
TypeMorph morph = new TypeMorph(
    MorphConfiguration.builder()
        .nullHandling(NullHandling.RETURN_NULL)        // null input → return null (not throw)
        .nullElementHandling(NullElementHandling.SKIP) // null list element → skip (not throw)
        .failOnMissingMapper(true)                     // default: true
        .failOnAmbiguousMapper(true)                   // default: true
        .build()
);
```

### `application.yml` Reference Table

| Property | Values | Default | Description |
|---|---|---|---|
| `null-handling` | `THROW_EXCEPTION`, `RETURN_NULL` | `THROW_EXCEPTION` | What to do when `null` is passed to `map()` |
| `null-element-handling` | `THROW_EXCEPTION`, `SKIP` | `THROW_EXCEPTION` | What to do when a `null` element appears in a list passed to `mapList()` |
| `fail-on-missing-mapper` | `true`, `false` | `true` | Throw `MorphNotFoundException` when no mapper is registered |
| `fail-on-ambiguous-mapper` | `true`, `false` | `true` | Throw `MorphAmbiguousException` when multiple targets registered for the same source |
| `enable-aop` | `true`, `false` | `true` | Register the `@TypeMorphAccepts` AOP aspect |
| `scan-packages` | `List<String>` | `[]` | Base packages to scan for `@TypeMorphClass` annotations at startup |

---

## Error Handling

All exceptions extend `MorphException extends RuntimeException` — no forced try/catch.

### Registry / mapping exceptions

| Exception | When thrown | Resolution |
|---|---|---|
| `MorphNotFoundException` | No mapping registered for the source type | Register a mapping or call `scan()` |
| `MorphAmbiguousException` | Multiple targets registered; `map(source)` cannot determine which to use | Use `map(source, TargetClass.class)` |
| `MorphConversionException` | The mapping function itself threw | Fix the lambda/mapping logic; cause preserved via `getCause()` |
| `MorphNullSourceException` | `null` passed to `map()` with `THROW_EXCEPTION` config | Pass non-null, or configure `RETURN_NULL` |
| `MorphTypeMismatchException` | Call-site inferred type `T` doesn't match the registered target type | Ensure registration and call-site types are consistent |
| `MorphNullElementException` | `null` element in list passed to `mapList()` | Filter nulls first, or configure `NullElementHandling.SKIP` |
| `MorphDuplicateRegistrationException` | `register()` called twice for the same `(sourceType, targetType)` pair | Each pair can only be registered once |
| `MorphTypeResolutionException` | `register(mapping)` called with a lambda (no explicit types) | Use `register(Source.class, Target.class, lambda)` |

### Reflective mapping exceptions (annotation-driven)

| Exception | When thrown | Resolution |
|---|---|---|
| `MorphConfigurationException` | Invalid annotation config at scan time: final POJO target field, `SET_NULL` on primitive, incompatible field types, no constructor | Fix the annotated class before startup |
| `MorphFieldMappingException` | A field read/write fails at runtime, or `NullFieldBehavior.THROW` triggers | Check field accessibility; review null behavior config |
| `MorphFieldTypeException` | Runtime type conversion fails (e.g., `"abc"` → `int`) | Ensure source data is valid for the target field type |
| `MorphInstantiationException` | Target class cannot be instantiated | Add a no-arg constructor or `@TypeMorphConstructor` |
| `MorphCircularReferenceException` | Circular object reference detected during `deepMap` | Add `@TypeMorphIgnore` to one side of the cycle |

### Example

```java
try {
    UserDto dto = morph.map(entity, UserDto.class);
} catch (MorphNotFoundException e) {
    log.warn("No mapper for {}", e.getSourceType().getSimpleName());
} catch (MorphFieldMappingException e) {
    log.error("Field '{}' failed: {} → {}",
        e.getFieldName(),
        e.getSourceType().getSimpleName(),
        e.getTargetType().getSimpleName());
} catch (MorphConversionException e) {
    log.error("Mapping failed: {}", e.getCause().getMessage());
}

// Or use mapSafe() to avoid exceptions for missing/null cases
Optional<UserDto> dto = morph.mapSafe(entity);
```

---

## Project Structure

```
type-morph/
├── pom.xml                                          # Parent POM (Java 17, Spring Boot BOM 3.x)
│
├── type-morph-core/                                 # Pure Java — no Spring dependencies
│   └── src/main/java/io/typemorph/
│       ├── TypeMorph.java                           # Main API: register(), scan(), map(), ensureType()
│       ├── annotation/
│       │   ├── MorphMapper.java
│       │   ├── TypeMorphClass.java                  # @TypeMorphClass(targets, bidirectional)
│       │   ├── TypeMorphField.java                  # @TypeMorphField(name, deepMap, onNull, target)
│       │   ├── TypeMorphFields.java                 # Container for @Repeatable TypeMorphField
│       │   ├── TypeMorphIgnore.java                 # @TypeMorphIgnore(target)
│       │   ├── TypeMorphIgnores.java                # Container for @Repeatable TypeMorphIgnore
│       │   └── TypeMorphConstructor.java            # @TypeMorphConstructor
│       ├── config/
│       │   ├── MorphConfiguration.java
│       │   ├── NullHandling.java
│       │   ├── NullElementHandling.java
│       │   └── NullFieldBehavior.java               # SKIP | SET_NULL | THROW
│       ├── exception/
│       │   ├── MorphException.java                  # Base (extends RuntimeException)
│       │   ├── MorphNotFoundException.java
│       │   ├── MorphAmbiguousException.java
│       │   ├── MorphConversionException.java
│       │   ├── MorphNullSourceException.java
│       │   ├── MorphTypeMismatchException.java
│       │   ├── MorphNullElementException.java
│       │   ├── MorphDuplicateRegistrationException.java
│       │   ├── MorphTypeResolutionException.java
│       │   ├── MorphConfigurationException.java     # Fail-fast scan-time errors
│       │   ├── MorphFieldMappingException.java      # Runtime field read/write errors
│       │   ├── MorphFieldTypeException.java         # Incompatible field type conversion
│       │   ├── MorphInstantiationException.java     # Cannot create target instance
│       │   └── MorphCircularReferenceException.java # Circular reference in deepMap
│       ├── mapping/
│       │   ├── MorphMapping.java                    # @FunctionalInterface map(S) → T
│       │   ├── BidirectionalMorphMapping.java
│       │   └── TypePair.java
│       ├── reflect/                                 # Annotation-driven reflective engine
│       │   ├── AnnotationMorphScanner.java          # Processes @TypeMorphClass, calls builder
│       │   ├── ReflectiveMappingBuilder.java        # Validates + builds FieldBinding[] at scan time
│       │   ├── ReflectiveMorphMapping.java          # Executes field-level mapping at runtime
│       │   ├── FieldBinding.java                    # Immutable: sourceField, targetField, converter...
│       │   ├── InstantiationStrategy.java           # Interface: newInstance(args)
│       │   ├── NoArgInstantiationStrategy.java      # Uses no-arg constructor
│       │   ├── RecordInstantiationStrategy.java     # Uses canonical record constructor
│       │   ├── AnnotatedConstructorInstantiationStrategy.java  # Uses @TypeMorphConstructor
│       │   ├── CycleGuard.java                      # ThreadLocal IdentityHashMap cycle detection
│       │   ├── TypeConverter.java                   # Interface: canConvert(), convert()
│       │   └── DefaultTypeConverter.java            # Built-in type conversions
│       ├── registry/
│       │   ├── MorphRegistry.java
│       │   └── DefaultMorphRegistry.java            # ConcurrentHashMap — thread-safe
│       └── util/GenericTypeResolver.java
│
└── type-morph-spring-boot-starter/                  # Spring Boot 3.x auto-configuration
    └── src/main/java/io/typemorph/spring/
        ├── annotation/
        │   ├── EnableTypeMorph.java
        │   └── TypeMorphAccepts.java                # @TypeMorphAccepts(TargetType.class)
        ├── aspect/
        │   └── TypeMorphMethodAspect.java           # @Aspect: converts @TypeMorphAccepts params
        └── autoconfigure/
            ├── TypeMorphAutoConfiguration.java      # @AutoConfiguration wiring
            └── TypeMorphProperties.java             # Binds typemorph.* properties
```

---

## Design Notes

### Fail-fast validation

All reflective mapping configuration is validated at **scan time**, not at the first `map()` call. Problems surface at application startup with precise messages:

- Final POJO target field → `MorphConfigurationException` (use a record or add `@TypeMorphConstructor`)
- `SET_NULL` on a primitive target field → `MorphConfigurationException`
- Incompatible source/target field types without `deepMap` → `MorphConfigurationException`
- Abstract or interface target class → `MorphConfigurationException`
- No accessible constructor → `MorphConfigurationException`

### Thread Safety

The registry uses a `ConcurrentHashMap` for lock-free reads. `register()` is `synchronized` to make the two-map update atomic — safe during Spring context startup. `map()` at runtime is fully concurrent with no locking.

`CycleGuard` uses a `ThreadLocal<IdentityHashMap>` — each thread tracks its own in-progress objects independently, making cycle detection thread-safe with no shared mutable state.

### The Unchecked Cast

`TypeMorph.map(Object source)` uses an unchecked cast `(T) result` internally. This is intentional and safe under one invariant: **the registered target type for a source type must match the inferred `T` at the call site.** If they mismatch, a `ClassCastException` is thrown at the assignment site and wrapped in `MorphTypeMismatchException`. Use `map(source, TargetClass.class)` for guaranteed type safety.

### CGLIB Proxy Considerations (`@TypeMorphAccepts`)

`@TypeMorphAccepts` works through Spring AOP CGLIB proxies. When a proxied method sets instance fields and you need to read them back, use **return values** rather than reading fields directly from the proxy reference — CGLIB proxies may maintain a separate target object whose fields are distinct from the proxy reference's fields. Return values always travel back through the proxy call chain correctly.

### Why Not MapStruct or ModelMapper?

| | type-morph | MapStruct | ModelMapper |
|---|---|---|---|
| Mapping definition | Lambda, named class, OR annotations | Annotation + code gen | Reflection / convention |
| Annotation-driven field binding | **Yes** | Yes (code gen) | Yes |
| Build-time code gen | No | Yes | No |
| Polymorphic `map()` | **Yes** | No | No |
| Spring auto-config | **Yes** | No | No |
| AOP method interception | **Yes** | No | No |
| Record support | **Yes** | Yes | Partial |
| Bidirectional | **Yes** | Partial | Yes |
| Automatic type conversion | **Yes** | Manual | Yes |
| Cycle detection | **Yes** | No | Partial |
| Zero deps (core) | **Yes** | Yes | No |
| Fail-fast validation | **Yes** | Yes (compile) | No |

`type-morph` is best when you want: annotation-driven convenience **plus** the option to write custom lambda logic — with a single `map()` call that works across all pairs — without annotation processors or build plugins.

---

## Building from Source

```bash
git clone https://github.com/your-org/type-morph.git
cd type-morph
mvn clean install
```

Run only the core tests:
```bash
mvn test -pl type-morph-core
```

Run only the Spring tests:
```bash
mvn test -pl type-morph-spring-boot-starter
```

**Current test coverage:** 78 tests, 0 failures across both modules.

---

## License

[Apache License 2.0](LICENSE)
