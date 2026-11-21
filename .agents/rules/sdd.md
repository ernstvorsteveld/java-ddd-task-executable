# Spec-Driven Development (Kiro SDD) Rules

All feature and behavioral work follows the **Kiro Spec-Driven Development** workflow under `.kiro/specs/<capability-name>/`.

## Mandatory 3-Phase Spec Gate

Never write tests or production code for a capability until all three Kiro spec artifacts exist and have been explicitly approved by the user:

1. **Requirements** (`.kiro/specs/<capability-name>/requirements.md`)
   - Captures user stories and numbered acceptance criteria written in **EARS** syntax (`WHEN ... the system shall ...`, `IF ... THEN ...`, `WHILE ...`, `The system shall ...`).
   - **Gate**: Present `requirements.md` and wait for explicit user approval before writing `design.md`.

2. **Design** (`.kiro/specs/<capability-name>/design.md`)
   - Maps requirements to the DDD/Hexagonal architecture: Ubiquitous Language, `domain` core model & invariants, `ports.in` / `ports.out` contracts, `application` orchestration, `adapters.in` / `adapters.out`, error handling, and JUnit 5 + ArchUnit test mapping.
   - **Gate**: Present `design.md` and wait for explicit user approval before writing `tasks.md`.

3. **Tasks** (`.kiro/specs/<capability-name>/tasks.md`)
   - Ordered, incremental TDD implementation checklist (`- [ ] 1.`, `- [ ] 1.1`), where every task references the requirement IDs it satisfies (e.g., `_Requirements: 1.1, 1.2_`).
   - **Gate**: Present `tasks.md` and wait for explicit user approval before writing any test or production code.

## Living Spec Maintenance

- **Organize by Domain Capability**: Group specs under `.kiro/specs/<capability-name>/` by lasting domain capability (e.g., `task-lifecycle`, `task-execution`), never by one-off tickets or PRs.
- **Single Source of Truth per Rule**: Each domain invariant or capability rule is owned by exactly one `requirements.md`; never duplicate or contradict requirements across two capability specs.
- **Shared Ubiquitous Language**: Maintain all domain terminology in a single shared `.kiro/specs/glossary.md` referenced by every capability spec.
- **Update In Place on Change or Bug Fix**: When existing behavior changes or a bug reveals an undocumented rule, update the owning capability's `requirements.md` (and `design.md` if architecture changes) in place with user approval, append a new incremental TDD section to `tasks.md`, and update the corresponding `ports.in` tests.
- **Clean Requirement IDs**: Remove obsolete criteria and renumber cleanly when requested, always syncing matching ID references in `design.md`, `tasks.md`, and `ports.in` tests.

## Execution Discipline

- Execute `tasks.md` strictly in order using TDD (Red -> Green -> Refactor).
- Mark tasks complete (`- [x]`) in `tasks.md` only after the corresponding tests pass and the behavior is verified.
- If a requirement or design assumption changes during implementation, stop and update the spec file(s) with user approval before deviating in code.
- Activate the `kiro-sdd` skill (`.agents/skills/kiro-sdd/SKILL.md`) when creating or updating a capability spec.
