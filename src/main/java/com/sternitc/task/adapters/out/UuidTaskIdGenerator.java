package com.sternitc.task.adapters.out;

import com.sternitc.task.domain.model.TaskId;
import com.sternitc.task.ports.out.TaskIdGenerator;
import java.util.UUID;

public final class UuidTaskIdGenerator implements TaskIdGenerator {

    @Override
    public TaskId nextId() {
        return new TaskId(UUID.randomUUID());
    }
}
