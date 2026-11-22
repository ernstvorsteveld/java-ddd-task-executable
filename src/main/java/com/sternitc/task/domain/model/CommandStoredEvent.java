package com.sternitc.task.domain.model;

import java.time.Instant;
import java.util.Objects;

public record CommandStoredEvent<T extends CommandPayload>(CommandId commandId, Instant occurredAt) {

    public CommandStoredEvent {
        Objects.requireNonNull(commandId, "commandId must not be null");
        Objects.requireNonNull(occurredAt, "occurredAt must not be null");
    }
}
