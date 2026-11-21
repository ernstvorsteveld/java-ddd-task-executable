package com.sternitc.task.domain.model;

import java.util.List;

public record LinkedEntities(List<LinkedEntity> entities) {

    private static final LinkedEntities EMPTY = new LinkedEntities(List.of());

    public LinkedEntities {
        if (entities == null) {
            throw new InvalidLinkedEntityException("entities must not be null");
        }
        for (LinkedEntity entity : entities) {
            if (entity == null) {
                throw new InvalidLinkedEntityException("linked entity element must not be null");
            }
        }
        entities = List.copyOf(entities);
    }

    public static LinkedEntities empty() {
        return EMPTY;
    }
}
