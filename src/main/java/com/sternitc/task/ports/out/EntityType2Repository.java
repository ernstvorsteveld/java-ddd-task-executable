package com.sternitc.task.ports.out;

import com.sternitc.task.domain.model.EntityId;
import com.sternitc.task.domain.model.EntityType2;
import java.util.Optional;

public interface EntityType2Repository {

    Optional<EntityType2> findById(EntityId entityId);
}
