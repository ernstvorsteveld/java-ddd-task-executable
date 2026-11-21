package com.sternitc.task.domain.model;

import java.util.Objects;

public final class Task implements Executable {

    private final TaskId id;
    private final TaskType type;
    private final TaskVersion version;
    private final TaskStatus status;
    private final ProcessName owner;
    private final PlanningWindow planningWindow;
    private final TaskExecutor executor;
    private final LinkedEntities linkedEntities;

    private Task(
            TaskId id,
            TaskType type,
            TaskVersion version,
            TaskStatus status,
            ProcessName owner,
            PlanningWindow planningWindow,
            TaskExecutor executor,
            LinkedEntities linkedEntities
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.type = Objects.requireNonNull(type, "type must not be null");
        this.version = Objects.requireNonNull(version, "version must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.owner = Objects.requireNonNull(owner, "owner must not be null");
        this.planningWindow = Objects.requireNonNull(planningWindow, "planningWindow must not be null");
        this.executor = Objects.requireNonNull(executor, "executor must not be null");
        this.linkedEntities = Objects.requireNonNull(linkedEntities, "linkedEntities must not be null");
    }

    public static Task plan(
            TaskId id,
            TaskType type,
            ProcessName owner,
            PlanningWindow planningWindow,
            TaskExecutor executor,
            LinkedEntities linkedEntities
    ) {
        return new Task(id, type, TaskVersion.initial(), TaskStatus.CREATED, owner, planningWindow, executor, linkedEntities);
    }

    public static Task reconstitute(
            TaskId id,
            TaskType type,
            TaskVersion version,
            TaskStatus status,
            ProcessName owner,
            PlanningWindow planningWindow,
            TaskExecutor executor,
            LinkedEntities linkedEntities
    ) {
        TaskVersion nextVersion = Objects.requireNonNull(version, "version must not be null").next();
        return new Task(id, type, nextVersion, status, owner, planningWindow, executor, linkedEntities);
    }

    public static Task restore(
            TaskId id,
            TaskType type,
            TaskVersion version,
            TaskStatus status,
            ProcessName owner,
            PlanningWindow planningWindow,
            TaskExecutor executor,
            LinkedEntities linkedEntities
    ) {
        return new Task(id, type, version, status, owner, planningWindow, executor, linkedEntities);
    }

    public Task update(TaskStatus newStatus, PlanningWindow newPlanningWindow, TaskExecutor newExecutor) {
        if (newStatus == null) {
            throw new InvalidTaskStatusException("status must not be null");
        }
        if (newPlanningWindow == null) {
            throw new InvalidPlanningWindowException("planningWindow must not be null");
        }
        if (newExecutor == null) {
            throw new InvalidTaskExecutorException("executor must not be null");
        }
        return reconstitute(id, type, version, newStatus, owner, newPlanningWindow, newExecutor, linkedEntities);
    }

    public TaskId id() {
        return id;
    }

    public TaskType type() {
        return type;
    }

    public TaskVersion version() {
        return version;
    }

    public TaskStatus status() {
        return status;
    }

    public ProcessName owner() {
        return owner;
    }

    public PlanningWindow planningWindow() {
        return planningWindow;
    }

    public TaskExecutor executor() {
        return executor;
    }

    public LinkedEntities linkedEntities() {
        return linkedEntities;
    }

    @Override
    public boolean isExecutable(ExecutableContext context) {
        Objects.requireNonNull(context, "context must not be null");
        return context.evaluate(this);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof Task that)) {
            return false;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
