package com.sternitc.task.application;

import com.sternitc.task.domain.model.CommandId;
import com.sternitc.task.domain.model.StoredCommand;
import com.sternitc.task.ports.in.CommandQuery;
import com.sternitc.task.ports.out.CommandRepository;
import java.util.Objects;
import java.util.Optional;

public final class CommandQueryService implements CommandQuery {

    private final CommandRepository commandRepository;

    public CommandQueryService(CommandRepository commandRepository) {
        this.commandRepository = Objects.requireNonNull(commandRepository, "commandRepository must not be null");
    }

    @Override
    public Optional<StoredCommand> findById(CommandId commandId) {
        Objects.requireNonNull(commandId, "commandId must not be null");
        return commandRepository.findById(commandId);
    }
}
