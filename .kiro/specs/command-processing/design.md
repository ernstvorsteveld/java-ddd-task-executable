# Design: Command Processing

## Domain Core (`com.sternitc.task.domain.model`)
- `CommandId(UUID value)` — non-null.
- `CommandReceipt(CommandId commandId, Instant timestamp)` — non-null.
- `enum CommandExecutionStatus { PENDING, COMPLETED, FAILED }`
- `interface CommandPayload` — generic, extensible domain supertype for all persisted command payloads.
- `sealed interface TaskCommandPayload extends CommandPayload permits TaskCommandPayload.PlanTask, TaskCommandPayload.UpdateTask` — domain-validated task command payloads.
- `StoredCommand` (Aggregate Root) — identity on `CommandId`; holds `timestamp`, `CommandPayload`, `CommandExecutionStatus`, optional `failureReason` (`Optional<String> failureReason()`); factories `accepted(id, timestamp, payload)` and `reconstitute(...)`; transitions `markCompleted()` and `markFailed(reason)`.
- `CommandStoredEvent<T extends CommandPayload>(CommandId commandId, Instant occurredAt)` — generic past-tense domain event typed to its `CommandPayload`.

## Ports (`com.sternitc.task.ports`)
- `ports.in`:
  - `PlanTaskUseCase`: `CommandReceipt planTask(PlanTaskCommand command)`
  - `UpdateTaskUseCase`: `CommandReceipt updateTask(UpdateTaskCommand command)`
  - `CommandQuery`: `Optional<StoredCommand> findById(CommandId commandId)`
- `ports.out`:
  - `CommandRepository`: `StoredCommand save(StoredCommand command)`, `Optional<StoredCommand> findById(CommandId commandId)`
  - `CommandEventPublisher<T extends CommandPayload>`: `void publish(CommandStoredEvent<T> event)`
  - `CommandIdGenerator`: `CommandId nextId()`

## Application (`com.sternitc.task.application`)
- `PlanTaskService implements PlanTaskUseCase` — validates `PlanTaskCommand`, persists `StoredCommand.accepted(...)` via `CommandRepository`, publishes `CommandStoredEvent<TaskCommandPayload.PlanTask>` via `CommandEventPublisher<TaskCommandPayload.PlanTask>`, returns `CommandReceipt`.
- `UpdateTaskService implements UpdateTaskUseCase` — validates `UpdateTaskCommand`, persists `StoredCommand.accepted(...)` via `CommandRepository`, publishes `CommandStoredEvent<TaskCommandPayload.UpdateTask>` via `CommandEventPublisher<TaskCommandPayload.UpdateTask>`, returns `CommandReceipt`.
- `CommandQueryService implements CommandQuery` — retrieves `StoredCommand` by `CommandId`.
- Dedicated per-command listeners (replacing monolithic `CommandStoredEventHandler`):
  - `PlanTaskCommandListener` (`CommandRepository`, `TaskRepository`, `TaskIdGenerator`) — async `handle(CommandStoredEvent<TaskCommandPayload.PlanTask>)`: loads `StoredCommand`, creates & persists `Task`, marks `StoredCommand` `COMPLETED` (or `FAILED` on domain error / payload mismatch).
  - `UpdateTaskCommandListener` (`CommandRepository`, `TaskRepository`) — async `handle(CommandStoredEvent<TaskCommandPayload.UpdateTask>)`: loads `StoredCommand`, updates & persists `Task`, marks `StoredCommand` `COMPLETED` (or `FAILED` on `TaskNotFoundException` / domain error / payload mismatch).

## Adapters (`com.sternitc.task.adapters.out`)
- `mongo/MongoCommandRepository implements CommandRepository` — stores BSON documents in MongoDB `commands` collection for `StoredCommand` (`CommandPayload`).
- `mongo/MongoTaskRepository implements TaskRepository` — stores BSON documents in MongoDB `tasks` collection (paginated `findByExecutor` & `search`).
- `async/VirtualThreadCommandEventBus<T extends CommandPayload> implements CommandEventPublisher<T>, AutoCloseable` — instantiated once per command payload type (`VirtualThreadCommandEventBus<TaskCommandPayload.PlanTask>`, `VirtualThreadCommandEventBus<TaskCommandPayload.UpdateTask>`, etc.), dispatching `CommandStoredEvent<T>` asynchronously on Java 27 Virtual Threads.
- `UuidCommandIdGenerator implements CommandIdGenerator`.

## Testing
- `ports.in` tests (`PlanTaskUseCaseTest`, `UpdateTaskUseCaseTest`, `TaskQueryTest`, `CommandQueryTest` extending `InboundPortTestSupport`) — wire separate typed `VirtualThreadCommandEventBus` instances per command payload type (`PlanTask` and `UpdateTask`) against real `MongoCommandRepository` and `MongoTaskRepository` (Testcontainers MongoDB `mongo:8.0`) + Awaitility, verifying `1.1`–`1.2` and `2.1`–`2.3` including isolation across typed buses/listeners.
- `architecture.HexagonalArchitectureTest` & `architecture.TestBoundaryArchitectureTest` — verify hexagonal boundaries and test-boundary rules remain enforced.
