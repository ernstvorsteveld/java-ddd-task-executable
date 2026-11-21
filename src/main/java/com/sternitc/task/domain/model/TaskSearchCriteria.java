package com.sternitc.task.domain.model;

import java.util.Objects;
import java.util.Optional;

public final class TaskSearchCriteria {

    private static final TaskSearchCriteria EMPTY = new TaskSearchCriteria(null, null, null, null);

    private final TaskExecutor executor;
    private final TaskStatus status;
    private final TaskType type;
    private final ProcessName owner;

    private TaskSearchCriteria(TaskExecutor executor, TaskStatus status, TaskType type, ProcessName owner) {
        this.executor = executor;
        this.status = status;
        this.type = type;
        this.owner = owner;
    }

    public static TaskSearchCriteria empty() {
        return EMPTY;
    }

    public TaskSearchCriteria withExecutor(TaskExecutor executor) {
        Objects.requireNonNull(executor, "executor must not be null");
        return new TaskSearchCriteria(executor, status, type, owner);
    }

    public TaskSearchCriteria withStatus(TaskStatus status) {
        Objects.requireNonNull(status, "status must not be null");
        return new TaskSearchCriteria(executor, status, type, owner);
    }

    public TaskSearchCriteria withType(TaskType type) {
        Objects.requireNonNull(type, "type must not be null");
        return new TaskSearchCriteria(executor, status, type, owner);
    }

    public TaskSearchCriteria withOwner(ProcessName owner) {
        Objects.requireNonNull(owner, "owner must not be null");
        return new TaskSearchCriteria(executor, status, type, owner);
    }

    public Optional<TaskExecutor> executor() {
        return Optional.ofNullable(executor);
    }

    public Optional<TaskStatus> status() {
        return Optional.ofNullable(status);
    }

    public Optional<TaskType> type() {
        return Optional.ofNullable(type);
    }

    public Optional<ProcessName> owner() {
        return Optional.ofNullable(owner);
    }

    public boolean matches(Task task) {
        Objects.requireNonNull(task, "task must not be null");
        if (executor != null && !executor.equals(task.executor())) {
            return false;
        }
        if (status != null && status != task.status()) {
            return false;
        }
        if (type != null && type != task.type()) {
            return false;
        }
        return owner == null || owner == task.owner();
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof TaskSearchCriteria that)) {
            return false;
        }
        return Objects.equals(executor, that.executor)
                && status == that.status
                && type == that.type
                && owner == that.owner;
    }

    @Override
    public int hashCode() {
        return Objects.hash(executor, status, type, owner);
    }
}
