package com.sternitc.task.ports.out;

import com.sternitc.task.domain.model.CommandId;
import com.sternitc.task.domain.model.StoredCommand;
import java.util.Optional;

public interface CommandRepository {

    StoredCommand save(StoredCommand command);

    Optional<StoredCommand> findById(CommandId commandId);
}
