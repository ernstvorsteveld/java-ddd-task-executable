# Testing & TDD Rules

1. **Test Only Through Inbound Boundaries**
   - **1A. Inbound Ports (`ports.in`)**
     - Write domain behavioral tests exclusively against `ports.in` interfaces.
     - **At least one test class per inbound port**: create at least one dedicated test class file under `src/test/java/com/sternitc/task/ports/in/` for every `ports.in` use-case or query interface (e.g., `<InboundPortName>Test.java`), sharing Testcontainers lifecycle and real adapter wiring via a shared abstract base support class (`InboundPortTestSupport.java`) with per-test database isolation.
     - **No isolated unit tests**: do not write standalone unit tests targeting internal `domain` classes, value objects, or `adapters.out` directly. All domain invariants, state transitions, and outbound integrations are exercised through `ports.in` or HTTP API tests.
     - **No direct domain behavior calls in tests**: never invoke domain behavior, state-transition, or evaluation methods (e.g., `task.update(...)`, `task.isExecutable(...)`, `context.evaluate(...)`) directly inside tests—even on domain objects returned by `ports.in`.
     - Even when an EARS requirement describes a domain-level contract, trigger and verify it exclusively by invoking `ports.in` use cases or queries (tests may construct input value objects/commands/queries, wire real components, and read returned domain getters for assertions only).
   - **1B. HTTP APIs (`adapters.in.rest`)**
     - Every HTTP REST API defined in the project must always have a dedicated API test under `src/test/java/com/sternitc/task/adapters/in/rest/` (e.g., `<ControllerName>Test.java`) implemented using **REST Assured**.
     - Verify the full HTTP contract—status codes (`200`, `202`, `4xx`, `5xx`), request/response payloads, query parameters, and uniform `ErrorResponsePayload`—over real HTTP wired against the real `application`, `domain`, and `adapters.out` layers (using Testcontainers/WireMock, with zero code mocking).

2. **Outside-In Test-Driven Development (TDD)**
   - Always follow the Red -> Green -> Refactor cycle at the `ports.in` boundary:
     1. **Red**: Write a focused, failing `ports.in` test specifying the desired behavior from the Kiro spec first.
     2. **Green**: Implement the minimal `ports`, `domain`, `application`, and `adapters.out` code required to make the test pass.
     3. **Refactor**: Improve domain design and clarity while keeping all tests green.
   - Do not write production behavior before writing the failing `ports.in` test that specifies it.

3. **Executable Specifications with JUnit 5 & AssertJ**
   - Map every Kiro EARS acceptance criterion in `.kiro/specs/<feature>/requirements.md` 1-to-1 to expressive JUnit 5 tests using `@Nested` and `@DisplayName`.
   - Use **AssertJ** for readable, behavior-focused assertions.
   - Reference the corresponding requirement ID where helpful for traceability.

4. **Zero Code Mocking — Real Infrastructure & WireMock**
   - Never mock Java classes or interfaces in code (no Mockito or other mocking frameworks, and no hand-written mock/fake implementations bypassing real adapters).
   - Wire the real `application`, `domain` core, and `adapters.out` implementations in `ports.in` and HTTP API tests.
   - For standard external components (e.g., databases, Kafka), spin up real instances using **Testcontainers**.
   - For external HTTP APIs, stub the HTTP boundary using **WireMock**.
   - *(Only add Testcontainers or WireMock dependencies to Gradle when a feature needs them, with explicit user approval per `workflow.md`.)*

5. **Architecture Verification (ArchUnit)**
   - Maintain an ArchUnit test suite verifying the cycle-free hexagonal dependency rules defined in `.agents/rules/architecture.md` (including verifying `adapters.out` has no dependency on `com.sternitc.task.api..` or `org.springframework..`).
   - Include ArchUnit checks over test classes (`src/test`) verifying behavioral tests reside under `com.sternitc.task.ports.in..` or `com.sternitc.task.adapters.in.rest..` (with architecture tests under `com.sternitc.task.architecture..`), every `ports.in` interface has at least one matching test class (`<InboundPortName>Test`), every HTTP REST API controller has at least one matching API test class (`<ControllerName>Test`), and tests do not call domain behavior/evaluation methods directly (never mask empty test scans with `allowEmptyShould(true)`).
   - Run architecture tests alongside `ports.in` and HTTP API tests on every change.
