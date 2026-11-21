package com.sternitc.task.adapters.out.inmemory;

import com.sternitc.task.domain.model.EntityId;
import com.sternitc.task.domain.model.EntityType3;
import com.sternitc.task.ports.out.EntityType3Repository;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryEntityType3Repository implements EntityType3Repository {

    private final Map<EntityId, EntityType3> store = new ConcurrentHashMap<>();

    public EntityType3 save(EntityType3 entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        store.put(entity.entityId(), entity);
        return entity;
    }

    @Override
    public Optional<EntityType3> findById(EntityId entityId) {
        Objects.requireNonNull(entityId, "entityId must not be null");
        return Optional.ofNullable(store.get(entityId));
    }
}
