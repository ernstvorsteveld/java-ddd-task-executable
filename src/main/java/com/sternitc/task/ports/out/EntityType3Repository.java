package com.sternitc.task.ports.out;

import com.sternitc.task.domain.model.EntityId;
import com.sternitc.task.domain.model.EntityType3;
import java.util.Optional;

public interface EntityType3Repository {

    Optional<EntityType3> findById(EntityId entityId);
}
