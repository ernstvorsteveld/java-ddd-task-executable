package com.sternitc.task.ports.out;

import com.sternitc.task.domain.model.CommandId;

public interface CommandIdGenerator {

    CommandId nextId();
}
