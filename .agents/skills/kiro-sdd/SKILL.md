---
name: kiro-sdd
description: >-
  Runbook for Kiro Spec-Driven Development in this DDD/Hexagonal Java 27 project.
  Use this skill whenever starting a new feature, refining requirements, designing
  domain/ports/adapters changes, or executing tasks from `.kiro/specs/<feature-name>/`.
---

# Kiro Spec-Driven Development Runbook

Follow this 4-phase workflow strictly for every feature. Never skip an approval gate.

## Directory Convention

Specs are organized as **living domain capabilities** under `.kiro/specs/`, sharing a single Ubiquitous Language glossary:

```text
.kiro/specs/
├── glossary.md              # Shared Ubiquitous Language across all capabilities
└── <capability-name>/
    ├── requirements.md      # Phase 1: User stories + EARS acceptance criteria
    ├── design.md            # Phase 2: DDD model, ports, application, adapters, test mapping
    └── tasks.md             # Phase 3: Ordered TDD task checklist linked to requirement IDs
```

---

## Phase 1: Requirements (`requirements.md`)

1. Use the template in [requirements-template.md](./resources/requirements-template.md).
2. Capture clear user stories (`As a ..., I want ..., so that ...`).
3. Express every acceptance criterion in **EARS** (Easy Approach to Requirements Syntax):
   - **Ubiquitous**: `The <system> shall <response>.`
   - **Event-driven**: `WHEN <trigger>, the <system> shall <response>.`
   - **State-driven**: `WHILE <state>, the <system> shall <response>.`
   - **Unwanted behavior**: `IF <condition>, THEN the <system> shall <response>.`
   - **Optional**: `WHERE <feature>, the <system> shall <response>.`
4. Number every requirement and criterion (`1.1`, `1.2`, `2.1`, ...) so `design.md`, `tasks.md`, and JUnit tests can reference them directly.
5. **Approval Gate**: Present `requirements.md` and ask the user for explicit approval before moving to Phase 2.

---

## Phase 2: Design (`design.md`)

1. Use the template in [design-template.md](./resources/design-template.md).
2. Map each requirement onto the hexagonal architecture under `com.sternitc.task`:
   - **Ubiquitous Language & Domain Core** (`domain`): aggregates, entities, value objects, domain invariants, domain events, domain exceptions.
   - **Ports** (`ports.in` & `ports.out`): inbound use-case interfaces, commands/queries, outbound SPI interfaces.
   - **Application Services** (`application`): use-case orchestration implementing `ports.in` and calling `ports.out`.
   - **Adapters** (`adapters.in` & `adapters.out`): driving and driven adapters.
   - **Testing Strategy**: 1-to-1 mapping from EARS criteria to JUnit 5 `@Nested`/`@DisplayName` + AssertJ tests exercising `ports.in` only (no unit tests, no code mocking; Testcontainers for DBs/Kafka, WireMock for external HTTP APIs), plus ArchUnit boundary rules.
3. Verify zero dependency cycles (`adapters -> ports -> domain core`; `application -> ports + domain core`).
4. **Approval Gate**: Present `design.md` and ask the user for explicit approval before moving to Phase 3.

---

## Phase 3: Tasks (`tasks.md`)

1. Use the template in [tasks-template.md](./resources/tasks-template.md).
2. Break the design into small, ordered outside-in TDD slices driven through `ports.in`:
   - Define `ports.in` / `ports.out` contracts and test harness (real wiring + Testcontainers for DBs/Kafka and/or WireMock for external HTTP APIs if needed)
   - For each capability/EARS requirement group: write failing `ports.in` tests first (Red), then implement `domain` core, `application`, and real `adapters.out` to pass (Green), then refactor
   - Implement driving adapters (`adapters.in`) if needed
   - Verify ArchUnit hexagonal boundary rules pass
3. Every task must reference its requirement IDs (e.g., `_Requirements: 1.1, 1.2_`).
4. **Approval Gate**: Present `tasks.md` and ask the user for explicit approval before writing any test or production code.

---

## Phase 4: TDD Execution

For each unchecked task in `tasks.md`, strictly in order:

1. **Plan**: Briefly state the files to create/edit for the task (per `.agents/rules/workflow.md`).
2. **Red**: Write the failing JUnit 5 + AssertJ test(s) corresponding to the referenced EARS acceptance criteria.
3. **Green**: Write the minimal Java 27 production code to make the test(s) pass.
4. **Refactor**: Clean up design/readability while keeping all tests green.
5. **Verify**: Ask the user for permission before running Gradle test commands (per `.agents/rules/safety.md`).
6. **Check Off**: Update `- [ ]` to `- [x]` in `.kiro/specs/<capability-name>/tasks.md`.
7. **Sync Glossary**: Ensure any new or refined domain terms are captured in `.kiro/specs/glossary.md`.

---

## Updating Existing Capabilities

When behavior changes or a bug reveals an undocumented rule:

1. **Locate the owning capability** under `.kiro/specs/<capability-name>/` (never duplicate the same invariant in a second capability).
2. **Update `requirements.md` in place** (keep existing EARS IDs stable; add new IDs or mark obsolete ones `*(Retired)*`), sync `.kiro/specs/glossary.md` if terms changed, and obtain user approval.
3. **Update `design.md` in place** if domain types, ports, or adapters change, and obtain user approval.
4. **Append an incremental TDD section** to `tasks.md` referencing the added/changed requirement IDs, and obtain user approval.
5. **Execute via outside-in TDD** through `ports.in`, updating or retiring the corresponding JUnit 5 `@DisplayName` tests.
