package com.sternitc.task.adapters.out;

import com.sternitc.task.domain.model.CommandId;
import com.sternitc.task.ports.out.CommandIdGenerator;
import java.util.UUID;

public final class UuidCommandIdGenerator implements CommandIdGenerator {

    @Override
    public CommandId nextId() {
        return new CommandId(UUID.randomUUID());
    }
}
