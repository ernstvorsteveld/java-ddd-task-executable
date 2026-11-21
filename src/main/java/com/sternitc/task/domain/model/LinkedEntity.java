package com.sternitc.task.domain.model;

public record LinkedEntity(EntityTypeName typeName, EntityId entityId) {

    public LinkedEntity {
        if (typeName == null) {
            throw new InvalidLinkedEntityException("typeName must not be null");
        }
        if (entityId == null) {
            throw new InvalidLinkedEntityException("entityId must not be null");
        }
    }
}
