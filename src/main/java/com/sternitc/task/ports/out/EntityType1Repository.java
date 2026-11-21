package com.sternitc.task.ports.out;

import com.sternitc.task.domain.model.EntityId;
import com.sternitc.task.domain.model.EntityType1;
import java.util.Optional;

public interface EntityType1Repository {

    Optional<EntityType1> findById(EntityId entityId);
}
