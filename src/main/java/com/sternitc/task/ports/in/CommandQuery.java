package com.sternitc.task.ports.in;

import com.sternitc.task.domain.model.CommandId;
import com.sternitc.task.domain.model.StoredCommand;
import java.util.Optional;

public interface CommandQuery {

    Optional<StoredCommand> findById(CommandId commandId);
}
