package com.sternitc.task.domain.model;

import java.util.Objects;

public sealed interface TaskCommandPayload permits TaskCommandPayload.PlanTask, TaskCommandPayload.UpdateTask {

    record PlanTask(
            TaskType type,
            ProcessName owner,
            PlanningWindow planningWindow,
            TaskExecutor executor,
            LinkedEntities linkedEntities
    ) implements TaskCommandPayload {

        public PlanTask {
            if (type == null) {
                throw new InvalidTaskTypeException("type must not be null");
            }
            if (owner == null) {
                throw new InvalidProcessNameException("owner must not be null");
            }
            if (planningWindow == null) {
                throw new InvalidPlanningWindowException("planningWindow must not be null");
            }
            if (executor == null) {
                throw new InvalidTaskExecutorException("executor must not be null");
            }
            if (linkedEntities == null) {
                throw new InvalidLinkedEntityException("linkedEntities must not be null");
            }
        }
    }

    record UpdateTask(
            TaskId taskId,
            TaskStatus status,
            PlanningWindow planningWindow,
            TaskExecutor executor
    ) implements TaskCommandPayload {

        public UpdateTask {
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
}
