package com.sternitc.task.domain.model;

import java.util.Objects;

public record EntityType1(EntityId entityId) implements DomainObject {

    public EntityType1 {
        Objects.requireNonNull(entityId, "entityId must not be null");
    }

    @Override
    public EntityTypeName typeName() {
        return EntityTypeName.ENTITY_TYPE_1;
    }
}
