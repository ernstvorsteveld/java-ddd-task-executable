package com.sternitc.task.domain.model;

import java.util.Objects;
import java.util.UUID;

public record CommandId(UUID value) {

    public CommandId {
        Objects.requireNonNull(value, "value must not be null");
    }
}
