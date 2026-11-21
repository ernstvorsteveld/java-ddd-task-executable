package com.sternitc.task.domain.model;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public record LoadedDomainObjects(Map<LinkedEntity, DomainObject> objects) {

    private static final LoadedDomainObjects EMPTY = new LoadedDomainObjects(Map.of());

    public LoadedDomainObjects {
        Objects.requireNonNull(objects, "objects must not be null");
        Map<LinkedEntity, DomainObject> validated = new LinkedHashMap<>();
        for (Map.Entry<LinkedEntity, DomainObject> entry : objects.entrySet()) {
            LinkedEntity reference = Objects.requireNonNull(entry.getKey(), "reference must not be null");
            DomainObject domainObject = Objects.requireNonNull(entry.getValue(), "domainObject must not be null");
            if (!reference.equals(domainObject.reference())) {
                throw new InvalidLinkedEntityException(
                        "Loaded domain object reference " + domainObject.reference() + " does not match key " + reference
                );
            }
            validated.put(reference, domainObject);
        }
        objects = Map.copyOf(validated);
    }

    public static LoadedDomainObjects empty() {
        return EMPTY;
    }

    public static LoadedDomainObjects of(Collection<? extends DomainObject> domainObjects) {
        Objects.requireNonNull(domainObjects, "domainObjects must not be null");
        if (domainObjects.isEmpty()) {
            return EMPTY;
        }
        Map<LinkedEntity, DomainObject> map = new LinkedHashMap<>();
        for (DomainObject domainObject : domainObjects) {
            Objects.requireNonNull(domainObject, "domainObject must not be null");
            map.put(domainObject.reference(), domainObject);
        }
        return new LoadedDomainObjects(map);
    }

    public Optional<DomainObject> find(LinkedEntity reference) {
        Objects.requireNonNull(reference, "reference must not be null");
        return Optional.ofNullable(objects.get(reference));
    }

    public Optional<DomainObject> find(EntityTypeName typeName, EntityId entityId) {
        return find(new LinkedEntity(typeName, entityId));
    }

    public List<DomainObject> findByType(EntityTypeName typeName) {
        Objects.requireNonNull(typeName, "typeName must not be null");
        return objects.values().stream()
                .filter(object -> object.typeName() == typeName)
                .toList();
    }

    public <T extends DomainObject> List<T> findByType(EntityTypeName typeName, Class<T> type) {
        Objects.requireNonNull(typeName, "typeName must not be null");
        Objects.requireNonNull(type, "type must not be null");
        return objects.values().stream()
                .filter(object -> object.typeName() == typeName)
                .filter(type::isInstance)
                .map(type::cast)
                .toList();
    }

    public boolean contains(LinkedEntity reference) {
        Objects.requireNonNull(reference, "reference must not be null");
        return objects.containsKey(reference);
    }

    public boolean isEmpty() {
        return objects.isEmpty();
    }
}
