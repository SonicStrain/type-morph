# Changelog

All notable changes to this project will be documented in this file.

---

## [1.0.0-SNAPSHOT] — 2026-04-26

### Summary

Introduces a second, zero-boilerplate mapping style alongside the existing lambda/named-class
approach. Instead of writing conversion logic manually, you annotate your source class and call
`morph.scan()` — TypeMorph inspects every non-static field at startup, validates the entire
configuration, and generates optimised field-level bindings that are reused for every subsequent
`map()` call.

A complementary Spring AOP layer (`@TypeMorphAccepts`) is also added, solving the compile-time
wall that previously forced method overloads or explicit conversion calls at every call site.

---

### Added

#### New Annotations (`type-morph-core`)

##### `@TypeMorphClass`
Placed on a **source class**. Declares one or more target types the class can be automatically
mapped to.

```java
@TypeMorphClass(targets = { UserDto.class, UserSummary.class })
public class UserEntity { ... }
```

| Attribute | Default | Purpose |
|---|---|---|
| `targets` | *(required)* | Target class(es) to map to |
| `bidirectional` | `false` | Also register the reverse mapping (target → source) automatically |

---

##### `@TypeMorphField`
Placed on a **source field**. Customises how that field is mapped. Repeatable — a different
annotation instance can be used for each target.

```java
@TypeMorphField(target = OrderDto.class,     name = "orderId")
@TypeMorphField(target = OrderSummary.class, name = "id")
private String referenceCode;

@TypeMorphField(onNull = NullFieldBehavior.SET_NULL)
private String notes;

@TypeMorphField(deepMap = true)
private AddressEntity address;
```

| Attribute | Default | Purpose |
|---|---|---|
| `target` | `Void.class` *(wildcard — all targets)* | Scope this annotation to one specific target |
| `name` | `""` *(same name)* | Target field name when it differs from the source field name |
| `deepMap` | `false` | Recursively convert the value through `TypeMorph.map()` |
| `onNull` | `NullFieldBehavior.SKIP` | What to do when the source field value is `null` |

---

##### `@TypeMorphIgnore`
Placed on a **source field**. Excludes the field from mapping entirely. Repeatable and
target-aware — the same field can be ignored for one target but still mapped to another.

```java
@TypeMorphIgnore(target = PublicDto.class)   // ignored only for PublicDto
@TypeMorphIgnore                             // Void.class sentinel = all targets
private String passwordHash;
```

---

##### `@TypeMorphConstructor`
Placed on a **constructor** of the **target class**. TypeMorph calls this constructor with
`null`/default arguments to create the instance when no no-arg constructor is available, then
sets each field individually via reflection.

```java
public class AddressDto {
    @TypeMorphConstructor
    public AddressDto(String street, String city) { ... }
}
```

---

#### New Enum: `NullFieldBehavior`

Controls per-field null handling, set via `@TypeMorphField(onNull = ...)`.

| Value | Behaviour |
|---|---|
| `SKIP` | *(default)* Leave the target field at its initialised value |
| `SET_NULL` | Actively write `null` to the target field (forbidden on primitive targets — caught at scan time) |
| `THROW` | Throw `MorphFieldMappingException` immediately |

---

#### New Reflective Engine (`type-morph-core` · `reflect` package)

All classes in this package are built once at scan time and reused for every `map()` call.
No annotation scanning or type resolution happens at mapping time.

| Class | Role |
|---|---|
| `ReflectiveMappingBuilder` | Reads annotations, validates everything, produces `FieldBinding[]` + `InstantiationStrategy` |
| `ReflectiveMorphMapping` | Executes field-level mapping at runtime using pre-built bindings |
| `FieldBinding` | Immutable record: sourceField, targetField, componentIndex, deepMap, onNull, converter |
| `InstantiationStrategy` | Interface for target instantiation |
| `NoArgInstantiationStrategy` | Calls the no-arg constructor; fields set after |
| `RecordInstantiationStrategy` | Collects all component values, calls the canonical constructor once |
| `AnnotatedConstructorInstantiationStrategy` | Calls `@TypeMorphConstructor` with null/defaults; fields set after |
| `CycleGuard` | `ThreadLocal<IdentityHashMap>` — detects circular references per thread |
| `TypeConverter` | Interface: `canConvert()` + `convert()` |
| `DefaultTypeConverter` | Built-in conversions — see table below |
| `AnnotationMorphScanner` | Processes `@TypeMorphClass` classes, calls `ReflectiveMappingBuilder`, registers results |

##### Built-in Type Conversions (`DefaultTypeConverter`)

| Conversion | Example |
|---|---|
| Primitive ↔ Wrapper | `int` ↔ `Integer` |
| Numeric widening | `int` → `long`, `int` → `double` |
| Numeric narrowing | `long` → `int`, `double` → `float` |
| Number → String | `42` → `"42"` |
| String → Number | `"42"` → `42` |
| Boolean → String | `true` → `"true"` |
| String → Boolean | `"true"` → `true` |
| Enum → String | `Color.RED` → `"RED"` |
| String → Enum | `"RED"` → `Color.RED` |

If no built-in conversion exists and `deepMap` is `false`, a `MorphConfigurationException` is
thrown **at scan time** — not at the first `map()` call.

---

#### New Exceptions (`type-morph-core`)

Five new typed exceptions extend `MorphException`, each carrying structured fields for
programmatic error handling.

| Exception | Thrown when | Key fields |
|---|---|---|
| `MorphConfigurationException` | Invalid annotation config detected at scan time (final target field, `SET_NULL` on primitive, incompatible types, no constructor, abstract target) | message, cause |
| `MorphFieldMappingException` | A field read/write fails at runtime, or `NullFieldBehavior.THROW` triggers | `sourceType`, `targetType`, `fieldName` |
| `MorphFieldTypeException` | Runtime value conversion fails (e.g. `"abc"` → `int`) | `sourceType`, `targetType` |
| `MorphInstantiationException` | Target class cannot be instantiated at runtime | `targetType` |
| `MorphCircularReferenceException` | Circular object reference detected during `deepMap` | `cycleType` |

---

#### New `TypeMorph` API Methods

##### `scan(Class<?>... annotatedClasses)`
Entry point for annotation-driven mapping. Processes `@TypeMorphClass` annotations and
registers reflective field mappings. Configuration errors surface immediately.

```java
TypeMorph morph = new TypeMorph()
    .scan(UserEntity.class, OrderEntity.class, ProductEntity.class);
```

Non-annotated classes passed to `scan()` are silently ignored.

##### `ensureType(Object input, Class<T> targetType)`
Returns `input` unchanged if it is already an instance of `targetType`. Otherwise, converts it
via `map(input, targetType)`. Useful at method boundaries to accept any registered source type
without changing the public API.

```java
OrderDto dto = morph.ensureType(input, OrderDto.class);
// works whether input is an OrderDto or an OrderEntity (or any other mapped type)
```

---

#### Java Record Support

Records work as both source and target with no special treatment required.

- **Record as target** — TypeMorph resolves component positions and calls the canonical
  constructor once with all values collected. Final backing fields are never written to directly.
- **Record as source** — TypeMorph reads the backing fields of the record (made accessible via
  `setAccessible`) the same way it reads POJO fields.
- **Bidirectional** — `@TypeMorphClass(bidirectional = true)` on a POJO registers both POJO →
  Record and Record → POJO automatically.

---

#### Fail-Fast Validation

`ReflectiveMappingBuilder` catches every configuration problem at scan time:

| Problem | Exception |
|---|---|
| Target is an interface or abstract class | `MorphConfigurationException` |
| Target POJO field is `final` | `MorphConfigurationException` |
| `SET_NULL` on a primitive target field | `MorphConfigurationException` |
| Source/target field types incompatible and `deepMap = false` | `MorphConfigurationException` |
| No no-arg constructor and no `@TypeMorphConstructor` | `MorphConfigurationException` |
| Field not accessible (`setAccessible` fails) | `MorphConfigurationException` |
| Duplicate `@TypeMorphField` for the same target on one field | `MorphConfigurationException` |

---

#### Spring AOP Integration (`type-morph-spring-boot-starter`)

##### `@TypeMorphAccepts`
A method-parameter annotation that signals the AOP aspect to auto-convert the argument before
the method body runs.

```java
public OrderDto process(@TypeMorphAccepts(OrderDto.class) Object order) {
    OrderDto dto = (OrderDto) order;  // AOP guarantees this cast is safe
    return dto;
}

// Callers can now pass any registered source type:
service.process(orderDto);      // passed through unchanged
service.process(orderEntity);   // automatically converted to OrderDto
```

This solves the **compile-time wall**: Java's type system prevents `methodName(classC)` when
the signature is `methodName(ClassB)`. `@TypeMorphAccepts` sidesteps this by widening the
parameter to `Object` while preserving full runtime type safety through the aspect.

##### `TypeMorphMethodAspect`
An `@Aspect` bean that backs `@TypeMorphAccepts`. Intercepts all public method calls on
Spring-managed beans, checks each parameter for `@TypeMorphAccepts`, and calls `ensureType()`
on any annotated parameter. If no annotation is present, the method proceeds with zero overhead
beyond a parameter inspection.

##### New `TypeMorphProperties` fields

```yaml
typemorph:
  scan-packages:          # packages to scan for @TypeMorphClass at startup
    - com.example.entity
    - com.example.dto
  enable-aop: true        # register TypeMorphMethodAspect (default: true)
```

##### Updated `TypeMorphAutoConfiguration`

- After registering `MorphMapping` beans, scans `scan-packages` (if configured) using Spring's
  `ClassPathScanningCandidateComponentProvider` — works correctly in packaged JARs and respects
  the classpath index.
- Registers `TypeMorphMethodAspect` as a bean when `enable-aop=true` (default). Backed off with
  `@ConditionalOnMissingBean(TypeMorphMethodAspect.class)` so users can provide their own
  implementation.

---

### Tests Added

**`type-morph-core`** — 5 new test classes, +32 tests:

| Test class | What it covers |
|---|---|
| `ReflectiveMappingBasicTest` | Same-name field mapping, `@TypeMorphField` rename, `@TypeMorphIgnore`, inherited fields, `ensureType` pass-through and conversion |
| `ReflectiveMappingAnnotationTest` | Per-target annotations, wildcard, all three `NullFieldBehavior` modes, all fail-fast scenarios |
| `TypeConverterTest` | Every built-in conversion path (9 type pairs), incompatible-type fail-fast at scan time |
| `RecordMappingTest` | Records as target, records as source, renamed components, partial mapping, bidirectional, fail-fast for missing no-arg constructor |
| `CycleDetectionTest` | `CycleGuard` semantics, ThreadLocal cleanup, cycle breaking with `@TypeMorphIgnore`, identity vs equality distinction |

**`type-morph-spring-boot-starter`** — 1 new test class, +5 tests:

| Test class | What it covers |
|---|---|
| `TypeMorphAopTest` | Aspect bean registration, transparent conversion via AOP, same-type pass-through, `enable-aop=false` disables the aspect, `scan-packages` registers `@TypeMorphClass` classes at startup |

**Total test count:** 78 tests, 0 failures across both modules.

---

### Files Changed

#### Added — `type-morph-core`

```
src/main/java/io/typemorph/annotation/TypeMorphClass.java
src/main/java/io/typemorph/annotation/TypeMorphField.java
src/main/java/io/typemorph/annotation/TypeMorphFields.java
src/main/java/io/typemorph/annotation/TypeMorphIgnore.java
src/main/java/io/typemorph/annotation/TypeMorphIgnores.java
src/main/java/io/typemorph/annotation/TypeMorphConstructor.java
src/main/java/io/typemorph/config/NullFieldBehavior.java
src/main/java/io/typemorph/exception/MorphConfigurationException.java
src/main/java/io/typemorph/exception/MorphFieldMappingException.java
src/main/java/io/typemorph/exception/MorphFieldTypeException.java
src/main/java/io/typemorph/exception/MorphInstantiationException.java
src/main/java/io/typemorph/exception/MorphCircularReferenceException.java
src/main/java/io/typemorph/reflect/AnnotationMorphScanner.java
src/main/java/io/typemorph/reflect/ReflectiveMappingBuilder.java
src/main/java/io/typemorph/reflect/ReflectiveMorphMapping.java
src/main/java/io/typemorph/reflect/FieldBinding.java
src/main/java/io/typemorph/reflect/InstantiationStrategy.java
src/main/java/io/typemorph/reflect/NoArgInstantiationStrategy.java
src/main/java/io/typemorph/reflect/RecordInstantiationStrategy.java
src/main/java/io/typemorph/reflect/AnnotatedConstructorInstantiationStrategy.java
src/main/java/io/typemorph/reflect/CycleGuard.java
src/main/java/io/typemorph/reflect/TypeConverter.java
src/main/java/io/typemorph/reflect/DefaultTypeConverter.java
src/test/java/io/typemorph/ReflectiveMappingBasicTest.java
src/test/java/io/typemorph/ReflectiveMappingAnnotationTest.java
src/test/java/io/typemorph/TypeConverterTest.java
src/test/java/io/typemorph/RecordMappingTest.java
src/test/java/io/typemorph/CycleDetectionTest.java
```

#### Modified — `type-morph-core`

```
src/main/java/io/typemorph/TypeMorph.java
  + import io.typemorph.reflect.AnnotationMorphScanner
  + scan(Class<?>... annotatedClasses)
  + ensureType(Object input, Class<T> targetType)
```

#### Added — `type-morph-spring-boot-starter`

```
src/main/java/io/typemorph/spring/annotation/TypeMorphAccepts.java
src/main/java/io/typemorph/spring/aspect/TypeMorphMethodAspect.java
src/test/java/io/typemorph/spring/TypeMorphAopTest.java
```

#### Modified — `type-morph-spring-boot-starter`

```
src/main/java/io/typemorph/spring/autoconfigure/TypeMorphProperties.java
  + List<String> scanPackages
  + boolean enableAop

src/main/java/io/typemorph/spring/autoconfigure/TypeMorphAutoConfiguration.java
  + import io.typemorph.reflect.AnnotationMorphScanner
  + typeMorphMethodAspect() @Bean
  + scanPackages() private helper (ClassPathScanningCandidateComponentProvider)

pom.xml
  + spring-boot-starter-aop (optional)
```

#### Documentation

```
README.md    — full rewrite covering all new features
CHANGELOG.md — this file
```

---

## [0.1.0] — Initial Release

- Lambda-based and named-class mapping via `TypeMorph.register()`
- Polymorphic `map(Object source)` with call-site type inference
- Explicit `map(source, TargetClass.class)` for type-safe disambiguation
- `mapList()` for collection mapping
- `mapSafe()` returning `Optional`
- `BidirectionalMorphMapping` interface
- 9 typed exceptions covering all failure scenarios
- `MorphConfiguration` with `NullHandling` and `NullElementHandling`
- Thread-safe `DefaultMorphRegistry` (`ConcurrentHashMap` + synchronized writes)
- `GenericTypeResolver` — resolves `MorphMapping<S,T>` type arguments from named classes
- Spring Boot 3.x auto-configuration (`TypeMorphAutoConfiguration`)
- `@EnableConfigurationProperties` binding for `typemorph.*` in `application.yml`
- `ObjectProvider<MorphMapping<?,?>>` with `orderedStream()` for zero-bean-safe discovery
- `ResolvableType` for CGLIB-proxy-safe type resolution in Spring context
- `@EnableTypeMorph` for non-Boot Spring applications
- 46 tests across both modules
