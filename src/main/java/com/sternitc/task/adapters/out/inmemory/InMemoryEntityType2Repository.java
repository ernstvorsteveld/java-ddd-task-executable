package com.sternitc.task.adapters.out.inmemory;

import com.sternitc.task.domain.model.EntityId;
import com.sternitc.task.domain.model.EntityType2;
import com.sternitc.task.ports.out.EntityType2Repository;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryEntityType2Repository implements EntityType2Repository {

    private final Map<EntityId, EntityType2> store = new ConcurrentHashMap<>();

    public EntityType2 save(EntityType2 entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        store.put(entity.entityId(), entity);
        return entity;
    }

    @Override
    public Optional<EntityType2> findById(EntityId entityId) {
        Objects.requireNonNull(entityId, "entityId must not be null");
        return Optional.ofNullable(store.get(entityId));
    }
}
