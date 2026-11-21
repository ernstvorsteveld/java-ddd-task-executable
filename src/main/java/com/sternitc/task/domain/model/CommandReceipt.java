package com.sternitc.task.domain.model;

import java.time.Instant;
import java.util.Objects;

public record CommandReceipt(CommandId commandId, Instant timestamp) {

    public CommandReceipt {
        Objects.requireNonNull(commandId, "commandId must not be null");
        Objects.requireNonNull(timestamp, "timestamp must not be null");
    }
}
