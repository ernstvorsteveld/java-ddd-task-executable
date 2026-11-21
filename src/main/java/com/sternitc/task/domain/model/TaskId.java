package com.sternitc.task.domain.model;

import java.util.Objects;
import java.util.UUID;

public record TaskId(UUID value) {

    public TaskId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
