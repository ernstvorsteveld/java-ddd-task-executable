package com.sternitc.task.ports.out;

import com.sternitc.task.domain.model.EntityId;
import com.sternitc.task.domain.model.EntityType4;
import java.util.Optional;

public interface EntityType4Repository {

    Optional<EntityType4> findById(EntityId entityId);
}
