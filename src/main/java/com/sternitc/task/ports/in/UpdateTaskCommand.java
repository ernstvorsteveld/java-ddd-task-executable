package com.sternitc.task.ports.in;

import com.sternitc.task.domain.model.InvalidPlanningWindowException;
import com.sternitc.task.domain.model.InvalidTaskExecutorException;
import com.sternitc.task.domain.model.InvalidTaskStatusException;
import com.sternitc.task.domain.model.PlanningWindow;
import com.sternitc.task.domain.model.TaskExecutor;
import com.sternitc.task.domain.model.TaskId;
import com.sternitc.task.domain.model.TaskStatus;
import java.util.Objects;

public record UpdateTaskCommand(
        TaskId taskId,
        TaskStatus status,
        PlanningWindow planningWindow,
        TaskExecutor executor
) {

    public UpdateTaskCommand {
        Objects.requireNonNull(taskId, "taskId must not be null");
        if (status == null) {
            throw new InvalidTaskStatusException("status must not be null");
        }
        if (planningWindow == null) {
            throw new InvalidPlanningWindowException("planningWindow must not be null");
        }
        if (executor == null) {
            throw new InvalidTaskExecutorException("executor must not be null");
        }
    }
}
