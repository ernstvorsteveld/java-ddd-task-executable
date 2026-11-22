package com.sternitc.task.application;

import com.sternitc.task.domain.model.CommandStoredEvent;
import com.sternitc.task.domain.model.InvalidLinkedEntityException;
import com.sternitc.task.domain.model.InvalidPlanningWindowException;
import com.sternitc.task.domain.model.InvalidProcessNameException;
import com.sternitc.task.domain.model.InvalidTaskExecutorException;
import com.sternitc.task.domain.model.InvalidTaskStatusException;
import com.sternitc.task.domain.model.InvalidTaskTypeException;
import com.sternitc.task.domain.model.InvalidTaskVersionException;
import com.sternitc.task.domain.model.StoredCommand;
import com.sternitc.task.domain.model.Task;
import com.sternitc.task.domain.model.TaskCommandPayload;
import com.sternitc.task.domain.model.TaskId;
import com.sternitc.task.ports.out.CommandRepository;
import com.sternitc.task.ports.out.TaskIdGenerator;
import com.sternitc.task.ports.out.TaskRepository;
import java.util.Objects;
import java.util.Optional;

public final class PlanTaskCommandListener {

    private final CommandRepository commandRepository;
    private final TaskRepository taskRepository;
    private final TaskIdGenerator taskIdGenerator;

    public PlanTaskCommandListener(
            CommandRepository commandRepository,
            TaskRepository taskRepository,
            TaskIdGenerator taskIdGenerator
    ) {
        this.commandRepository = Objects.requireNonNull(commandRepository, "commandRepository must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.taskIdGenerator = Objects.requireNonNull(taskIdGenerator, "taskIdGenerator must not be null");
    }

    public void handle(CommandStoredEvent<TaskCommandPayload.PlanTask> event) {
        Objects.requireNonNull(event, "event must not be null");
        Optional<StoredCommand> maybeCommand = commandRepository.findById(event.commandId());
        if (maybeCommand.isEmpty()) {
            return;
        }
        StoredCommand stored = maybeCommand.orElseThrow();
        if (!(stored.payload() instanceof TaskCommandPayload.PlanTask planTask)) {
            commandRepository.save(stored.markFailed(
                    "Unexpected command payload for PlanTaskCommandListener: " + stored.payload().getClass().getSimpleName()
            ));
            return;
        }
        try {
            TaskId id = taskIdGenerator.nextId();
            Task task = Task.plan(
                    id,
                    planTask.type(),
                    planTask.owner(),
                    planTask.planningWindow(),
                    planTask.executor(),
                    planTask.linkedEntities()
            );
            taskRepository.save(task);
            commandRepository.save(stored.markCompleted());
        } catch (InvalidTaskTypeException
                 | InvalidTaskVersionException
                 | InvalidTaskStatusException
                 | InvalidProcessNameException
                 | InvalidPlanningWindowException
                 | InvalidTaskExecutorException
                 | InvalidLinkedEntityException ex) {
            String reason = Objects.requireNonNullElse(ex.getMessage(), ex.getClass().getSimpleName());
            commandRepository.save(stored.markFailed(reason));
        }
    }
}
