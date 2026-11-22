package com.sternitc.task.application;

import com.sternitc.task.domain.model.CommandReceipt;
import com.sternitc.task.domain.model.CommandStoredEvent;
import com.sternitc.task.domain.model.StoredCommand;
import com.sternitc.task.domain.model.TaskCommandPayload;
import com.sternitc.task.ports.in.UpdateTaskCommand;
import com.sternitc.task.ports.in.UpdateTaskUseCase;
import com.sternitc.task.ports.out.CommandEventPublisher;
import com.sternitc.task.ports.out.CommandIdGenerator;
import com.sternitc.task.ports.out.CommandRepository;
import java.time.Clock;
import java.util.Objects;

public final class UpdateTaskService implements UpdateTaskUseCase {

    private final CommandRepository commandRepository;
    private final CommandEventPublisher<TaskCommandPayload.UpdateTask> commandEventPublisher;
    private final CommandIdGenerator commandIdGenerator;
    private final Clock clock;

    public UpdateTaskService(
            CommandRepository commandRepository,
            CommandEventPublisher<TaskCommandPayload.UpdateTask> commandEventPublisher,
            CommandIdGenerator commandIdGenerator,
            Clock clock
    ) {
        this.commandRepository = Objects.requireNonNull(commandRepository, "commandRepository must not be null");
        this.commandEventPublisher = Objects.requireNonNull(commandEventPublisher, "commandEventPublisher must not be null");
        this.commandIdGenerator = Objects.requireNonNull(commandIdGenerator, "commandIdGenerator must not be null");
        this.clock = Objects.requireNonNull(clock, "clock must not be null");
    }

    @Override
    public CommandReceipt updateTask(UpdateTaskCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        var payload = new TaskCommandPayload.UpdateTask(
                command.taskId(),
                command.status(),
                command.planningWindow(),
                command.executor()
        );
        StoredCommand stored = StoredCommand.accepted(commandIdGenerator.nextId(), clock.instant(), payload);
        commandRepository.save(stored);
        commandEventPublisher.publish(new CommandStoredEvent<>(stored.id(), stored.timestamp()));
        return stored.receipt();
    }
}
