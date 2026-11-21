package com.sternitc.task.adapters.out.inmemory;

import com.sternitc.task.domain.model.EntityId;
import com.sternitc.task.domain.model.EntityType4;
import com.sternitc.task.ports.out.EntityType4Repository;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryEntityType4Repository implements EntityType4Repository {

    private final Map<EntityId, EntityType4> store = new ConcurrentHashMap<>();

    public EntityType4 save(EntityType4 entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        store.put(entity.entityId(), entity);
        return entity;
    }

    @Override
    public Optional<EntityType4> findById(EntityId entityId) {
        Objects.requireNonNull(entityId, "entityId must not be null");
        return Optional.ofNullable(store.get(entityId));
    }
}
