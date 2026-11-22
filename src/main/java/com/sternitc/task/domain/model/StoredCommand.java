package com.sternitc.task.domain.model;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public final class StoredCommand {

    private final CommandId id;
    private final Instant timestamp;
    private final CommandPayload payload;
    private final CommandExecutionStatus status;
    private final String failureReason;

    private StoredCommand(
            CommandId id,
            Instant timestamp,
            CommandPayload payload,
            CommandExecutionStatus status,
            String failureReason
    ) {
        this.id = Objects.requireNonNull(id, "id must not be null");
        this.timestamp = Objects.requireNonNull(timestamp, "timestamp must not be null");
        this.payload = Objects.requireNonNull(payload, "payload must not be null");
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.failureReason = failureReason;
    }

    public static StoredCommand accepted(CommandId id, Instant timestamp, CommandPayload payload) {
        return new StoredCommand(id, timestamp, payload, CommandExecutionStatus.PENDING, null);
    }

    public static StoredCommand reconstitute(
            CommandId id,
            Instant timestamp,
            CommandPayload payload,
            CommandExecutionStatus status,
            String failureReason
    ) {
        return new StoredCommand(id, timestamp, payload, status, failureReason);
    }

    public StoredCommand markCompleted() {
        return new StoredCommand(id, timestamp, payload, CommandExecutionStatus.COMPLETED, null);
    }

    public StoredCommand markFailed(String reason) {
        Objects.requireNonNull(reason, "reason must not be null");
        return new StoredCommand(id, timestamp, payload, CommandExecutionStatus.FAILED, reason);
    }

    public CommandId id() {
        return id;
    }

    public Instant timestamp() {
        return timestamp;
    }

    public CommandPayload payload() {
        return payload;
    }

    public CommandExecutionStatus status() {
        return status;
    }

    public Optional<String> failureReason() {
        return Optional.ofNullable(failureReason);
    }

    public CommandReceipt receipt() {
        return new CommandReceipt(id, timestamp);
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof StoredCommand that)) {
            return false;
        }
        return id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
