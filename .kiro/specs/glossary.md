# Ubiquitous Language Glossary

Single source of truth for domain terminology used across `.kiro/specs/`, `com.sternitc.task.domain`, `com.sternitc.task.ports`, and `ports.in` tests.

| Term | Definition | Owning Capability |
| :--- | :--- | :--- |
| **Task** | Aggregate Root representing a planned unit of executable work with a `TaskId`, `TaskType`, `TaskVersion`, `TaskStatus`, owning `ProcessName`, `PlanningWindow`, `TaskExecutor`, and `LinkedEntities`. | `task-planning` |
| **TaskId** | Unique typed identity of a `Task`. | `task-planning` |
| **TaskType** | Controlled enum of allowed task types (`DO_AANVRAAG`, `DO_OPDRACHT`, `DO_OFFERTE`, `DO_CONTRACT`); translated into human-readable text outside this domain. | `task-planning` |
| **TaskVersion** | Positive integer (`>= 1`) initialized to `1` on creation and incremented by `1` on every reconstitution; not caller-settable. | `task-planning` |
| **TaskStatus** | Lifecycle state of a `Task` (`CREATED`, `IN_PROGRESS`, `PAUSED`, `CANCELLED`, `DONE`); initialized to `CREATED` on creation. | `task-planning` |
| **ProcessName** | Controlled enum identifying the process that owns a `Task` (`PS_IE`, `PS_UE`, `PO_BO`, `PO_VG`, `PO_UG`). | `task-planning` |
| **PlanningWindow** | Required pair of `Instant` deadlines (`startBefore` and `finishBefore`) where `startBefore` is not after `finishBefore`. | `task-planning` |
| **TaskExecutor** | Mutually exclusive assignment specifying who can execute a `Task`: either a single `User` (by `UserId`) or a single `Role` (by `RoleName`). | `task-planning` |
| **LinkedEntities** | Immutable value object holding zero or more `LinkedEntity` references attached to a `Task` at creation. | `task-planning` |
| **LinkedEntity** | Immutable reference (`EntityTypeName`, `EntityId`) linking a `Task` to a domain entity. | `task-planning` |
| **EntityTypeName** | Controlled enum of linked entity types (`ENTITY_TYPE_1`, `ENTITY_TYPE_2`, `ENTITY_TYPE_3`, `ENTITY_TYPE_4`). | `task-planning` |
| **EntityId** | Non-blank typed identifier of a linked entity. | `task-planning` |
| **Task Planning Service** | Inbound port boundary (`ports.in`) for creating and querying planned tasks. | `task-planning` |
| **CommandId** | Unique typed identity (`UUID`) of a stored command. | `command-processing` |
| **CommandReceipt** | Value object (`commandId`, `timestamp`) returned immediately when a command is validated, stored, and accepted for async handling. | `command-processing` |
| **CommandPayload** | Generic, extensible domain interface implemented by all persisted command payloads (e.g., `TaskCommandPayload`). | `command-processing` |
| **StoredCommand** | Persisted command record stored before asynchronous execution, carrying a `CommandPayload` and tracking execution outcome. | `command-processing` |
| **CommandStoredEvent** | Generic past-tense domain event (`CommandStoredEvent<T extends CommandPayload>`) published immediately after a command is persisted to trigger asynchronous handling on its typed channel. | `command-processing` |
| **Executable** | Domain interface implemented by `Task` providing `isExecutable(ExecutableContext)`. | `task-executability` |
| **ExecutableContext** | Domain interface passed to `isExecutable(ExecutableContext)`, holding `LoadedDomainObjects` and evaluated via a list of `Predicate<Task>` (implementable one or more times per `TaskType`, with a default implementation returning `true`). | `task-executability` |
| **DomainObject** | Sealed domain interface (`EntityType1`, `EntityType2`, `EntityType3`, `EntityType4`) representing a loaded linked entity or aggregate identified by `EntityTypeName` and `EntityId`. | `task-executability` |
| **LoadedDomainObjects** | Immutable value object holding loaded `DomainObject` instances keyed by `LinkedEntity` for predicate evaluation in `ExecutableContext`. | `task-executability` |
