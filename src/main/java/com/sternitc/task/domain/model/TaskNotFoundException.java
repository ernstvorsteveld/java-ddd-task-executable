package com.sternitc.task.domain.model;

public final class TaskNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TaskNotFoundException(TaskId taskId) {
        super("Task not found: " + taskId);
    }
}
