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
import com.sternitc.task.domain.model.TaskNotFoundException;
import com.sternitc.task.ports.out.CommandRepository;
import com.sternitc.task.ports.out.TaskRepository;
import java.util.Objects;
import java.util.Optional;

public final class UpdateTaskCommandListener {

    private final CommandRepository commandRepository;
    private final TaskRepository taskRepository;

    public UpdateTaskCommandListener(
            CommandRepository commandRepository,
            TaskRepository taskRepository
    ) {
        this.commandRepository = Objects.requireNonNull(commandRepository, "commandRepository must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
    }

    public void handle(CommandStoredEvent<TaskCommandPayload.UpdateTask> event) {
        Objects.requireNonNull(event, "event must not be null");
        Optional<StoredCommand> maybeCommand = commandRepository.findById(event.commandId());
        if (maybeCommand.isEmpty()) {
            return;
        }
        StoredCommand stored = maybeCommand.orElseThrow();
        if (!(stored.payload() instanceof TaskCommandPayload.UpdateTask updateTask)) {
            commandRepository.save(stored.markFailed(
                    "Unexpected command payload for UpdateTaskCommandListener: " + stored.payload().getClass().getSimpleName()
            ));
            return;
        }
        try {
            Task existing = taskRepository.findById(updateTask.taskId())
                    .orElseThrow(() -> new TaskNotFoundException(updateTask.taskId()));
            Task updated = existing.update(
                    updateTask.status(),
                    updateTask.planningWindow(),
                    updateTask.executor()
            );
            taskRepository.save(updated);
            commandRepository.save(stored.markCompleted());
        } catch (TaskNotFoundException
                 | InvalidTaskTypeException
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
