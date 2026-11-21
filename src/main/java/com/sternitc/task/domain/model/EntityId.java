package com.sternitc.task.domain.model;

public record EntityId(String value) {

    public EntityId {
        if (value == null || value.isBlank()) {
            throw new InvalidLinkedEntityException("entityId must not be null or blank");
        }
    }
}
