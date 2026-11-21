# Design: Task Planning

## Domain Core (`com.sternitc.task.domain.model`)
- `TaskId(UUID value)` — non-null.
- `enum TaskType { DO_AANVRAAG, DO_OPDRACHT, DO_OFFERTE, DO_CONTRACT }`
- `TaskVersion(int value)` — `value >= 1`; `TaskVersion.initial()` (`1`) and `next()` (`value + 1`); throws `InvalidTaskVersionException`.
- `enum TaskStatus { CREATED, IN_PROGRESS, PAUSED, CANCELLED, DONE }`
- `enum ProcessName { PS_IE, PS_UE, PO_BO, PO_VG, PO_UG }`
- `PlanningWindow(Instant startBefore, Instant finishBefore)` — both non-null, `!startBefore.isAfter(finishBefore)`; throws `InvalidPlanningWindowException`.
- `UserId(String value)`, `RoleName(String value)` — non-null, non-blank; throw `InvalidTaskExecutorException`.
- `sealed interface TaskExecutor permits TaskExecutor.User, TaskExecutor.Role` (`record User(UserId userId)`, `record Role(RoleName roleName)`).
- `enum EntityTypeName { ENTITY_TYPE_1, ENTITY_TYPE_2, ENTITY_TYPE_3, ENTITY_TYPE_4 }`
- `EntityId(String value)` — non-null, non-blank; throws `InvalidLinkedEntityException`.
- `LinkedEntity(EntityTypeName typeName, EntityId entityId)` — both non-null; throws `InvalidLinkedEntityException`.
- `LinkedEntities(List<LinkedEntity> entities)` — non-null list with no null elements (`List.copyOf`), `LinkedEntities.empty()`; throws `InvalidLinkedEntityException`.
- `TaskSearchCriteria` — immutable filter value object with `empty()`, `withExecutor(...)` / `withStatus(...)` / `withType(...)` / `withOwner(...)`, `Optional` accessors, and `matches(Task)`.
- `Task` (Aggregate Root) — identity on `TaskId`; private constructor takes `TaskVersion`, `TaskStatus`, and `LinkedEntities`; `Task.plan(...)` initializes `TaskVersion.initial()` (`1`) and `TaskStatus.CREATED` with `LinkedEntities`; `Task.reconstitute(...)` passes `version.next()`; `Task.restore(...)` preserves persisted version; `Task.update(status, planningWindow, executor)` validates and returns updated `Task` with `version.next()` (preserving `id`, `type`, `owner`, `linkedEntities`); read-only accessors, no setters.
- Domain exceptions: `InvalidTaskTypeException`, `InvalidTaskVersionException`, `InvalidTaskStatusException`, `InvalidProcessNameException`, `InvalidPlanningWindowException`, `InvalidTaskExecutorException`, `InvalidLinkedEntityException`, `InvalidPageRequestException`, `TaskNotFoundException`.

## Ports (`com.sternitc.task.ports`)
- `ports.in`:
  - `PlanTaskCommand(TaskType type, ProcessName owner, PlanningWindow planningWindow, TaskExecutor executor, LinkedEntities linkedEntities)`
  - `UpdateTaskCommand(TaskId taskId, TaskStatus status, PlanningWindow planningWindow, TaskExecutor executor)`
  - `FindTasksByExecutorQuery(TaskExecutor executor, int pageNumber, int pageSize)` — 0-based `pageNumber >= 0` (default `0`), `pageSize > 0` (default `20`); `of(TaskExecutor)` uses defaults
  - `SearchTasksQuery(TaskSearchCriteria criteria, int pageNumber, int pageSize)` — 0-based `pageNumber >= 0` (default `0`), `pageSize > 0` (default `20`); `of(TaskSearchCriteria)` uses defaults
  - `PlanTaskUseCase`: `CommandReceipt planTask(PlanTaskCommand command)`
  - `UpdateTaskUseCase`: `CommandReceipt updateTask(UpdateTaskCommand command)`
  - `TaskQuery`: `Optional<Task> findById(TaskId taskId)`, `List<Task> findByExecutor(FindTasksByExecutorQuery query)`, `List<Task> search(SearchTasksQuery query)`
- `ports.out`:
  - `TaskRepository`: `Task save(Task task)`, `Optional<Task> findById(TaskId taskId)`, `List<Task> findByExecutor(TaskExecutor executor, int pageNumber, int pageSize)`, `List<Task> search(TaskSearchCriteria criteria, int pageNumber, int pageSize)`
  - `TaskIdGenerator`: `TaskId nextId()`

## Application (`com.sternitc.task.application`)
- `PlanTaskService implements PlanTaskUseCase` — validates/stores `PlanTask` command and publishes `CommandStoredEvent`.
- `UpdateTaskService implements UpdateTaskUseCase` — validates/stores `UpdateTask` command and publishes `CommandStoredEvent`.
- `TaskQueryService implements TaskQuery` — queries tasks via `TaskRepository` and filters executable tasks via `ExecutableContextResolver`.
- `CommandQueryService implements CommandQuery` — queries stored commands via `CommandRepository`.
- `CommandStoredEventHandler` — handles `CommandStoredEvent` asynchronously to persist `Task` via `TaskRepository`.

## Adapters (`com.sternitc.task.adapters.out`)
- `mongo/MongoTaskRepository implements TaskRepository` — MongoDB-backed persistence with 0-based `pageNumber` and `pageSize` slicing for both `findByExecutor` and `search`.
- `UuidTaskIdGenerator implements TaskIdGenerator`.

## Testing
- `ports.in.TaskPlanningPortTest` — tests `PlanTaskUseCase`, `UpdateTaskUseCase`, `TaskQuery`, and `CommandQuery` with real `TaskPlanningService`, `MongoTaskRepository`, `MongoCommandRepository`, and `VirtualThreadCommandEventBus` against Testcontainers MongoDB (no unit tests, no mocks), mapped 1-to-1 to `1.1`–`1.8`, `2.1`–`2.3`, `3.1`–`3.4`.
- `architecture.HexagonalArchitectureTest` — enforces all hexagonal dependency rules.
