# type-morph

**Zero-boilerplate class mapping for Java — with polymorphic method support.**

`type-morph` is a lightweight Java library that eliminates the repetitive code required to map between two different classes (e.g., Entity ↔ DTO), and enables a single method to transparently handle multiple type pairs without overloading.

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

---

## The Solution

`type-morph` provides a central registry. Register your mappings once, then use a single `map()` call that figures out the right conversion at runtime — with full type safety.

```java
// Register your mappings
TypeMorph morph = new TypeMorph()
    .register(UserEntity.class, UserDto.class,   entity -> new UserDto(entity.getId(), entity.getFullName()))
    .register(OrderEntity.class, OrderDto.class, entity -> new OrderDto(entity.getId(), entity.getDescription()));

// Same method — different type pairs. No overloading needed.
UserDto  user  = morph.map(userEntity);   // returns UserDto
OrderDto order = morph.map(orderEntity);  // returns OrderDto — same map() call!
```

---

## Features

- **Lambda-friendly** — register mappings as one-liners
- **Polymorphic `map()`** — same method works for all registered type pairs
- **Bidirectional mapping** — define A→B and B→A together
- **Collection mapping** — `mapList()` converts entire lists
- **Safe mapping** — `mapSafe()` returns `Optional` instead of throwing
- **Fully typed exceptions** — 9 specific exception types for every failure scenario
- **Spring Boot auto-configuration** — define `@Component` mappers, get them auto-wired
- **`application.yml` configurable** — null handling, error policy, element skipping
- **Thread-safe registry** — safe for concurrent reads at runtime
- **Zero mandatory dependencies** — core module has no runtime dependencies

---

## Requirements

- Java 17+
- Maven 3.6+ or Gradle 7+
- Spring Boot 3.x *(only for the starter module — optional)*

---

## Installation

### Maven

Add to your `pom.xml`:

**Core only (no Spring):**
```xml
<dependency>
    <groupId>io.typemorph</groupId>
    <artifactId>type-morph-core</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Spring Boot starter (includes core):**
```xml
<dependency>
    <groupId>io.typemorph</groupId>
    <artifactId>type-morph-spring-boot-starter</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

### Gradle

**Core only:**
```groovy
implementation 'io.typemorph:type-morph-core:1.0.0-SNAPSHOT'
```

**Spring Boot starter:**
```groovy
implementation 'io.typemorph:type-morph-spring-boot-starter:1.0.0-SNAPSHOT'
```

---

## Quick Start

### Standalone (no Spring)

```java
import io.typemorph.TypeMorph;

// 1. Create a TypeMorph instance
TypeMorph morph = new TypeMorph();

// 2. Register mappings (lambda form — must provide explicit types)
morph.register(UserEntity.class, UserDto.class,
        entity -> new UserDto(entity.getId(), entity.getFirstName() + " " + entity.getLastName()));

morph.register(OrderEntity.class, OrderDto.class,
        entity -> new OrderDto(entity.getId(), entity.getDescription()));

// 3. Map objects — same method, different type pairs
UserDto  userDto  = morph.map(userEntity);   // → UserDto
OrderDto orderDto = morph.map(orderEntity);  // → OrderDto
```

### Spring Boot

**Step 1** — Add the starter dependency (see Installation above).

**Step 2** — Define your mappers as Spring beans:

```java
@Component
public class UserEntityMapper implements MorphMapping<UserEntity, UserDto> {
    @Override
    public UserDto map(UserEntity source) {
        return new UserDto(
            source.getId(),
            source.getFirstName() + " " + source.getLastName()
        );
    }
}

@Component
public class OrderEntityMapper implements MorphMapping<OrderEntity, OrderDto> {
    @Override
    public OrderDto map(OrderEntity source) {
        return new OrderDto(source.getId(), source.getDescription());
    }
}
```

**Step 3** — Inject and use `TypeMorph` anywhere:

```java
@Service
public class MyService {

    @Autowired
    private TypeMorph morph;

    public UserDto getUser(Long id) {
        UserEntity entity = userRepository.findById(id).orElseThrow();
        return morph.map(entity);           // → UserDto
    }

    public OrderDto getOrder(Long id) {
        OrderEntity entity = orderRepository.findById(id).orElseThrow();
        return morph.map(entity);           // → OrderDto (same method!)
    }
}
```

That's it. No XML, no annotation processors, no code generation.

---

## Core API Reference

### `TypeMorph` — Main Class

#### Registration

```java
// Form 1: Explicit types — works with lambdas, anonymous classes, named classes
morph.register(ClassB.class, ClassA.class, source -> new ClassA(source.getValue()));

// Form 2: Auto-resolves types via reflection — only works with named classes (not lambdas)
morph.register(new MyNamedMapper());  // MyNamedMapper implements MorphMapping<ClassB, ClassA>

// Supports fluent chaining
TypeMorph morph = new TypeMorph()
    .register(A.class, B.class, a -> new B(a.getX()))
    .register(C.class, D.class, c -> new D(c.getY()));
```

> **Why can't lambdas auto-register?**
> Java erases generic type information for lambdas at compile time. `source -> new ClassA(...)` produces a class that implements `MorphMapping` as a raw type — there's no way to recover `ClassB` or `ClassA` via reflection. Named classes that declare `implements MorphMapping<ClassB, ClassA>` preserve this information.

#### Single-object mapping

```java
// Inferred return type — same method, different pairs
ClassA a = morph.map(classB);
ClassC c = morph.map(classD);

// Explicit target type — always type-safe, resolves ambiguity
ClassA a = morph.map(classB, ClassA.class);

// Safe mapping — returns Optional instead of throwing
Optional<ClassA> opt = morph.mapSafe(classB);
ClassA a = morph.mapSafe(classB).orElse(defaultValue);
```

#### Collection mapping

```java
List<UserEntity> entities = userRepository.findAll();

// Inferred target type
List<UserDto> dtos = morph.mapList(entities);

// Explicit target type
List<UserDto> dtos = morph.mapList(entities, UserDto.class);
```

#### Inspection

```java
morph.canMap(UserEntity.class);                      // true/false
morph.canMap(UserEntity.class, UserDto.class);       // true/false
```

---

## Polymorphic Method Pattern

This is the core differentiator of `type-morph`. You can write one generic method in your service that handles all registered type pairs:

```java
@Service
public class EntityService {

    @Autowired
    private TypeMorph morph;

    /**
     * Single generic method — works for any registered source type.
     * The return type is inferred by the compiler from the call-site assignment.
     */
    public <T> T fetchAndConvert(Long id, Class<?> entityType) {
        Object entity = repository.findById(entityType, id);
        return morph.map(entity);  // registry determines the correct return type
    }
}

// Usage — same method, different types:
UserDto   user    = service.fetchAndConvert(1L, UserEntity.class);
OrderDto  order   = service.fetchAndConvert(2L, OrderEntity.class);
ProductDto product = service.fetchAndConvert(3L, ProductEntity.class);
```

**How it works:**

`TypeMorph.map(Object source)` has the signature `<T> T map(Object source)`. The JVM infers `T` from the variable declaration at the call site. The actual value returned comes from the registry — since `UserEntity → UserDto` was registered, `morph.map(userEntity)` returns a `UserDto` instance. The cast is safe as long as the call-site inferred type matches what the registry returns.

**Disambiguation when a source maps to multiple targets:**

```java
morph.register(UserEntity.class, UserDto.class,     u -> new UserDto(...));
morph.register(UserEntity.class, UserSummary.class, u -> new UserSummary(...));

// morph.map(userEntity)  → throws MorphAmbiguousException (multiple targets)
// Solution: use explicit target type
UserDto     full    = morph.map(userEntity, UserDto.class);
UserSummary summary = morph.map(userEntity, UserSummary.class);
```

---

## Bidirectional Mapping

Use `BidirectionalMorphMapping` to define both directions together, then register each direction separately:

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
// Basic list mapping
List<UserDto> dtos = morph.mapList(entityList);

// Null handling in collections
// Default: throws MorphNullElementException if any element is null
List<UserDto> dtos = morph.mapList(entityList);

// Configure to skip nulls instead:
TypeMorph morph = new TypeMorph(
    MorphConfiguration.builder()
        .nullElementHandling(NullElementHandling.SKIP)
        .build()
);
List<UserDto> dtos = morph.mapList(entityListWithNulls);  // nulls skipped silently
```

---

## Configuration

### Programmatic (standalone)

```java
TypeMorph morph = new TypeMorph(
    MorphConfiguration.builder()
        .nullHandling(NullHandling.RETURN_NULL)          // return null instead of throwing on null input
        .nullElementHandling(NullElementHandling.SKIP)   // skip null elements in lists
        .failOnMissingMapper(true)                       // throw if no mapper found (default: true)
        .failOnAmbiguousMapper(true)                     // throw if multiple targets (default: true)
        .build()
);
```

### Via `application.yml` (Spring Boot)

```yaml
typemorph:
  null-handling: RETURN_NULL          # RETURN_NULL | THROW_EXCEPTION (default)
  null-element-handling: SKIP         # SKIP | THROW_EXCEPTION (default)
  fail-on-missing-mapper: true        # default: true
  fail-on-ambiguous-mapper: true      # default: true
```

### Configuration Reference

| Property | Values | Default | Description |
|---|---|---|---|
| `null-handling` | `THROW_EXCEPTION`, `RETURN_NULL` | `THROW_EXCEPTION` | What to do when `null` is passed to `map()` |
| `null-element-handling` | `THROW_EXCEPTION`, `SKIP` | `THROW_EXCEPTION` | What to do when a `null` element appears in a list passed to `mapList()` |
| `fail-on-missing-mapper` | `true`, `false` | `true` | Throw `MorphNotFoundException` when no mapper is registered for the source type |
| `fail-on-ambiguous-mapper` | `true`, `false` | `true` | Throw `MorphAmbiguousException` when multiple targets are registered for the same source type |

---

## Error Handling

All exceptions extend `MorphException extends RuntimeException` — no forced try/catch.

| Exception | When it's thrown | Resolution |
|---|---|---|
| `MorphNotFoundException` | No mapping registered for the source type | Call `register()` for the source type |
| `MorphAmbiguousException` | Multiple targets registered; `map(source)` can't determine which to use | Use `map(source, TargetClass.class)` instead |
| `MorphConversionException` | The mapping function itself threw an exception | Fix the mapping logic; original cause is preserved via `getCause()` |
| `MorphNullSourceException` | `null` passed to `map()` with `THROW_EXCEPTION` config | Pass a non-null object, or configure `RETURN_NULL` |
| `MorphTypeMismatchException` | Call-site inferred type `T` doesn't match the registered target type | Ensure your registration and call-site types are consistent |
| `MorphNullElementException` | `null` element in a list passed to `mapList()` | Filter nulls first, or configure `NullElementHandling.SKIP` |
| `MorphDuplicateRegistrationException` | `register()` called twice for the same `(sourceType, targetType)` pair | Each pair can only be registered once |
| `MorphTypeResolutionException` | `register(mapping)` (no explicit types) called with a lambda | Use `register(SourceClass.class, TargetClass.class, lambda)` instead |

### Example: catching specific errors

```java
try {
    UserDto dto = morph.map(entity);
} catch (MorphNotFoundException e) {
    log.warn("No mapper for {}", e.getSourceType().getSimpleName());
} catch (MorphConversionException e) {
    log.error("Mapping failed for {} → {}: {}",
        e.getSourceType().getSimpleName(),
        e.getTargetType().getSimpleName(),
        e.getCause().getMessage());
}

// Or use mapSafe() to avoid exceptions entirely for missing/null cases
Optional<UserDto> dto = morph.mapSafe(entity);
```

---

## Spring Boot Integration Details

### Auto-registration

Any bean implementing `MorphMapping<S, T>` is automatically picked up:

```java
@Component
public class UserMapper implements MorphMapping<UserEntity, UserDto> {
    @Override
    public UserDto map(UserEntity source) {
        return new UserDto(source.getId(), source.getName());
    }
}
```

Spring Boot's auto-configuration scans the context for all `MorphMapping` beans and registers them in the `TypeMorph` instance before your application starts.

### Override the TypeMorph bean

To take full control, define your own `TypeMorph` bean — auto-configuration backs off automatically:

```java
@Configuration
public class MyMorphConfig {
    @Bean
    public TypeMorph typeMorph() {
        return new TypeMorph(
            MorphConfiguration.builder()
                .nullHandling(NullHandling.RETURN_NULL)
                .build()
        ).register(UserEntity.class, UserDto.class, u -> new UserDto(u.getId(), u.getName()));
    }
}
```

### Non-Boot Spring applications

Use `@EnableTypeMorph` to import the configuration explicitly:

```java
@Configuration
@EnableTypeMorph
public class AppConfig { }
```

---

## Project Structure

```
type-morph/
├── pom.xml                                          # Parent POM
│
├── type-morph-core/                                 # Pure Java — no Spring
│   └── src/main/java/io/typemorph/
│       ├── TypeMorph.java                           # Main API
│       ├── annotation/MorphMapper.java
│       ├── config/
│       │   ├── MorphConfiguration.java
│       │   ├── NullHandling.java
│       │   └── NullElementHandling.java
│       ├── exception/                               # 9 typed exceptions
│       │   ├── MorphException.java
│       │   ├── MorphNotFoundException.java
│       │   ├── MorphAmbiguousException.java
│       │   ├── MorphConversionException.java
│       │   ├── MorphNullSourceException.java
│       │   ├── MorphTypeMismatchException.java
│       │   ├── MorphNullElementException.java
│       │   ├── MorphDuplicateRegistrationException.java
│       │   └── MorphTypeResolutionException.java
│       ├── mapping/
│       │   ├── MorphMapping.java                    # Core functional interface
│       │   ├── BidirectionalMorphMapping.java
│       │   └── TypePair.java
│       ├── registry/
│       │   ├── MorphRegistry.java
│       │   └── DefaultMorphRegistry.java            # Thread-safe implementation
│       └── util/GenericTypeResolver.java
│
└── type-morph-spring-boot-starter/                  # Spring Boot 3.x
    └── src/main/java/io/typemorph/spring/
        ├── annotation/EnableTypeMorph.java
        └── autoconfigure/
            ├── TypeMorphAutoConfiguration.java
            └── TypeMorphProperties.java
```

---

## Design Notes

### Thread Safety

The registry uses a `ConcurrentHashMap` internally. `register()` is `synchronized` to make registration atomic — safe for use during Spring context startup. Read operations (`map()`, `canMap()`) are fully concurrent and lock-free, making them safe for high-throughput request handling.

### The Unchecked Cast

`TypeMorph.map(Object source)` uses an unchecked cast (`(T) result`) internally. This is intentional and safe under one invariant: **the registered target type for a source type must match the inferred type `T` at the call site**. The registry enforces this at registration time via Java generics. If you use raw types to bypass generics during registration, the cast will fail at the assignment site with a `ClassCastException`, wrapped in `MorphTypeMismatchException`.

### Why Not MapStruct or ModelMapper?

| | type-morph | MapStruct | ModelMapper |
|---|---|---|---|
| Mapping definition | Lambda / named class | Annotation + code gen | Reflection / convention |
| Build-time code gen | No | Yes | No |
| Polymorphic `map()` | **Yes** | No | No |
| Spring auto-config | **Yes** | No | No |
| Bidirectional | **Yes** | Partial | Yes |
| Type-safe | Yes | Yes | Partial |
| Zero deps (core) | **Yes** | Yes | No |

`type-morph` is best when you want: explicit control over mapping logic, a single `map()` call that works across type pairs, and Spring Boot auto-wiring — without annotation processors or build plugins.

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

---

## License

[Apache License 2.0](LICENSE)
