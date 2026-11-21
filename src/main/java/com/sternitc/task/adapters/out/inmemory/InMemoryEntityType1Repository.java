package com.sternitc.task.adapters.out.inmemory;

import com.sternitc.task.domain.model.EntityId;
import com.sternitc.task.domain.model.EntityType1;
import com.sternitc.task.ports.out.EntityType1Repository;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public final class InMemoryEntityType1Repository implements EntityType1Repository {

    private final Map<EntityId, EntityType1> store = new ConcurrentHashMap<>();

    public EntityType1 save(EntityType1 entity) {
        Objects.requireNonNull(entity, "entity must not be null");
        store.put(entity.entityId(), entity);
        return entity;
    }

    @Override
    public Optional<EntityType1> findById(EntityId entityId) {
        Objects.requireNonNull(entityId, "entityId must not be null");
        return Optional.ofNullable(store.get(entityId));
    }
}
