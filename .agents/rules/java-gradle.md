# Java 27 & Gradle Rules

## Build & Toolchain

- **Gradle Kotlin DSL**: Use `build.gradle.kts` and `settings.gradle.kts`.
- **Java Version**: Target Java 27 via the Gradle Java toolchain (`JavaLanguageVersion.of(27)`).
- **Strict Compiler Flags**: Compile all Java sources with UTF-8 encoding, `-Xlint:all`, `-Werror` (warnings fail the build), and `-parameters`.
- **Approved Dependencies** (do not add or change without explicit approval per `workflow.md`):
  - Plugins: **`org.openapi.generator`** (generates Spring REST server stubs into `build/generated/openapi`).
  - Production (`adapters` only; `domain` and `ports` stay pure JDK 27): **`org.springframework.boot:spring-boot-dependencies`** (BOM), **`spring-boot-starter-web`**, **`spring-boot-starter-validation`**, **`org.mongodb:mongodb-driver-sync`**.
  - Test only (currently configured): **JUnit 5 (Jupiter)**, **AssertJ**, **ArchUnit**, **Testcontainers (`testcontainers-junit-jupiter`, `testcontainers-mongodb`)**, **Awaitility**.
  - Test only (approved tools to add when a feature requires external systems, after asking): additional **Testcontainers** modules (e.g., Kafka) and **WireMock** (for external HTTP APIs).

## Domain & Type Modeling

- **No Primitive Obsession**: Wrap domain identifiers and meaningful domain concepts in dedicated Value Object `record`s (e.g., `TaskId`, `TaskTitle`) rather than passing raw `String`, `UUID`, or `Long` across domain and port boundaries.
- **Intention-Revealing Domain Methods**: Never expose public setters (`setX(...)`) on domain entities; model state changes as expressive domain methods (e.g., `complete(...)`, `cancel(...)`).
- **Immutability by Default**:
  - Use `record` for Value Objects, Commands, Queries, and Domain Events.
  - Make fields `private final` wherever possible and defensively copy collections with `List.copyOf`, `Set.copyOf`, or `Map.copyOf`.
- **Deterministic Time & IDs in `domain` Core**:
  - Never call `Instant.now()`, `LocalDate.now()`, `ZonedDateTime.now()`, or `UUID.randomUUID()` directly inside `domain` core logic.
  - Pass `Clock`, timestamps, or generated IDs into domain methods/factories so behavior remains deterministic.
- **Zero Framework Magic**: No Lombok, no reflection, and no framework annotations in `domain` or `ports`.

## Null Safety & Error Modeling

- **Never Return `null`**: Public methods and constructors must never return `null`.
- **Eager Validation**: Validate invariants and non-null inputs immediately in constructors / compact constructors using `Objects.requireNonNull(arg, "arg must not be null")` and explicit domain checks.
- **Strict `Optional<T>` Usage**:
  - Use `Optional<T>` only as return types for genuinely optional lookups — never as fields, required record components, or method parameters.
  - Never call `Optional.get()`; use `orElseThrow(...)`, `map(...)`, `flatMap(...)`, or `ifPresentOrElse(...)`.
- **Explicit Error Modeling**:
  - Model expected domain outcomes with `sealed` result hierarchies or expressive unchecked domain exceptions.
  - Never swallow exceptions or use broad `catch (Exception)` blocks in `domain` or `ports`.

## Modern Java 27 Idioms & Code Hygiene

- **Exhaustive Pattern Matching**: Use `sealed` interfaces/classes and `switch` expressions **without a `default` branch** over `sealed` types and `enum`s so the compiler catches unhandled variants.
- Use record patterns, guarded patterns, sequenced collections (`getFirst()`, `getLast()`), and `.toList()` (unmodifiable) over `.collect(Collectors.toList())`.
- Use `java.time` (`Instant`, `Duration`, `LocalDate`, `ZoneId`, `Clock`) exclusively — never legacy `java.util.Date` or `Calendar`.
- **No Wildcard Imports**: Avoid `import foo.*;` (static assertions in tests excepted only if explicit imports become noisy; prefer explicit imports everywhere).
- **Restrained `var`**: Use `var` only when the concrete type is immediately obvious on the right-hand side.
- **Concise Documentation**: Prefer clear, small, self-documenting code; write JavaDoc only for public port contracts and non-obvious domain invariants.

## Concurrency (Adapters Only)

- When concurrency is needed in adapters, prefer Virtual Threads (`Executors.newVirtualThreadPerTaskExecutor()`) and `ReentrantLock` over unmanaged thread pools or `synchronized` blocks around blocking I/O.
