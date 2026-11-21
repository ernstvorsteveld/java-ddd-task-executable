package com.sternitc.task.ports.in;

import com.sternitc.task.domain.model.InvalidLinkedEntityException;
import com.sternitc.task.domain.model.InvalidPlanningWindowException;
import com.sternitc.task.domain.model.InvalidProcessNameException;
import com.sternitc.task.domain.model.InvalidTaskExecutorException;
import com.sternitc.task.domain.model.InvalidTaskTypeException;
import com.sternitc.task.domain.model.LinkedEntities;
import com.sternitc.task.domain.model.PlanningWindow;
import com.sternitc.task.domain.model.ProcessName;
import com.sternitc.task.domain.model.TaskExecutor;
import com.sternitc.task.domain.model.TaskType;

public record PlanTaskCommand(
        TaskType type,
        ProcessName owner,
        PlanningWindow planningWindow,
        TaskExecutor executor,
        LinkedEntities linkedEntities
) {

    public PlanTaskCommand(
            TaskType type,
            ProcessName owner,
            PlanningWindow planningWindow,
            TaskExecutor executor
    ) {
        this(type, owner, planningWindow, executor, LinkedEntities.empty());
    }

    public PlanTaskCommand {
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
