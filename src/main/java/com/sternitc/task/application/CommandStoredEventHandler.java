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
import com.sternitc.task.domain.model.TaskNotFoundException;
import com.sternitc.task.ports.out.CommandRepository;
import com.sternitc.task.ports.out.TaskIdGenerator;
import com.sternitc.task.ports.out.TaskRepository;
import java.util.Objects;
import java.util.Optional;

public final class CommandStoredEventHandler {

    private final CommandRepository commandRepository;
    private final TaskRepository taskRepository;
    private final TaskIdGenerator taskIdGenerator;

    public CommandStoredEventHandler(
            CommandRepository commandRepository,
            TaskRepository taskRepository,
            TaskIdGenerator taskIdGenerator
    ) {
        this.commandRepository = Objects.requireNonNull(commandRepository, "commandRepository must not be null");
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.taskIdGenerator = Objects.requireNonNull(taskIdGenerator, "taskIdGenerator must not be null");
    }

    public void handle(CommandStoredEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        Optional<StoredCommand> maybeCommand = commandRepository.findById(event.commandId());
        if (maybeCommand.isEmpty()) {
            return;
        }
        StoredCommand stored = maybeCommand.orElseThrow();
        try {
            execute(stored.payload());
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

    private void execute(TaskCommandPayload payload) {
        switch (payload) {
            case TaskCommandPayload.PlanTask(var type, var owner, var planningWindow, var executor, var linkedEntities) -> {
                TaskId id = taskIdGenerator.nextId();
                Task task = Task.plan(id, type, owner, planningWindow, executor, linkedEntities);
                taskRepository.save(task);
            }
            case TaskCommandPayload.UpdateTask(var taskId, var status, var planningWindow, var executor) -> {
                Task existing = taskRepository.findById(taskId)
                        .orElseThrow(() -> new TaskNotFoundException(taskId));
                Task updated = existing.update(status, planningWindow, executor);
                taskRepository.save(updated);
            }
        }
    }
}
