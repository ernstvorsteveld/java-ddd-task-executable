# Requirements: Task Planning

## Requirement 1: Create Task
**User Story:** As a user, I want a task to know its controlled `TaskType`, `TaskVersion`, owning `ProcessName`, start-before and finish-before timestamps, and the user or role that can execute it, so we can plan tasks and know to whom to present them.

1.1. WHEN a task is created with a `TaskType`, owning `ProcessName`, `startBefore <= finishBefore`, one executor (`User` or `Role`), and `LinkedEntities`, the system shall persist and return the task with a unique `TaskId`, initial `TaskVersion` `1`, and initial `TaskStatus` `CREATED`.
1.2. IF `startBefore` or `finishBefore` is missing, or `startBefore` is after `finishBefore`, THEN the system shall reject creation.
1.3. IF the executor (`User` or `Role`) is missing or blank, THEN the system shall reject creation.
1.4. IF `TaskType` is missing (allowed: `DO_AANVRAAG`, `DO_OPDRACHT`, `DO_OFFERTE`, `DO_CONTRACT`), THEN the system shall reject creation.
1.5. IF owning `ProcessName` is missing (allowed: `PS_IE`, `PS_UE`, `PO_BO`, `PO_VG`, `PO_UG`), THEN the system shall reject creation.
1.6. The system shall initialize `TaskVersion` to `1` on task creation, increment it by `1` on every reconstitution, and not allow callers to set `TaskVersion` directly.
1.7. The system shall support `TaskStatus` values `CREATED`, `IN_PROGRESS`, `PAUSED`, `CANCELLED`, and `DONE`, initialize `TaskStatus` to `CREATED` on task creation, and not allow callers to set `TaskStatus` on creation.
1.8. The system shall allow a `Task` to hold `LinkedEntities` containing zero or more `LinkedEntity` items (`EntityTypeName` in `ENTITY_TYPE_1`, `ENTITY_TYPE_2`, `ENTITY_TYPE_3`, `ENTITY_TYPE_4` and non-blank `EntityId`), immutable after creation; IF `LinkedEntities` is null or any `LinkedEntity` has a missing `EntityTypeName` or blank `EntityId`, THEN the system shall reject creation.

## Requirement 2: Update Task
**User Story:** As a user, I want to update an existing task's status, planning window, and executor, so that task progress and scheduling stay up to date.

2.1. WHEN an update is submitted for an existing `TaskId` with valid `TaskStatus`, `PlanningWindow`, and `TaskExecutor`, the system shall update those fields, increment `TaskVersion` by `1`, persist, and return the updated task.
2.2. IF no task exists for the requested `TaskId`, THEN the system shall reject the update as not found.
2.3. IF `TaskStatus`, `PlanningWindow`, or `TaskExecutor` is missing or invalid, THEN the system shall reject the update.

## Requirement 3: Query & Search Tasks
**User Story:** As a user, I want to look up tasks by ID, by assigned user/role, or by optional filters with pagination, so we know which tasks to present to whom.

3.1. WHEN queried by `TaskId`, the system shall return the matching task or empty if not found.
3.2. WHEN queried by `User` or `Role` (`findByExecutor`) with 0-based `pageNumber` (default `0`) and `pageSize` (default `20`), the system shall return only the matching page of tasks assigned to that `User` or `Role`.
3.3. WHEN searched with optional `TaskExecutor`, `TaskStatus`, `TaskType`, and `ProcessName` filters (`search`) and 0-based `pageNumber` (default `0`) and `pageSize` (default `20`), the system shall return only the matching page of tasks satisfying all provided filters.
3.4. IF `pageNumber < 0` or `pageSize <= 0`, THEN the system shall reject the query or search.
