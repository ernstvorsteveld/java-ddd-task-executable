package com.sternitc.task.domain.model;

public sealed interface DomainObject permits EntityType1, EntityType2, EntityType3, EntityType4 {

    EntityTypeName typeName();

    EntityId entityId();

    default LinkedEntity reference() {
        return new LinkedEntity(typeName(), entityId());
    }
}
