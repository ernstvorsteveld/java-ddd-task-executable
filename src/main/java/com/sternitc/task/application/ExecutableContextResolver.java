package com.sternitc.task.application;

import com.sternitc.task.domain.model.DefaultExecutableContext;
import com.sternitc.task.domain.model.DomainObject;
import com.sternitc.task.domain.model.ExecutableContext;
import com.sternitc.task.domain.model.LinkedEntity;
import com.sternitc.task.domain.model.LoadedDomainObjects;
import com.sternitc.task.domain.model.Task;
import com.sternitc.task.domain.model.TaskType;
import com.sternitc.task.ports.out.EntityType1Repository;
import com.sternitc.task.ports.out.EntityType2Repository;
import com.sternitc.task.ports.out.EntityType3Repository;
import com.sternitc.task.ports.out.EntityType4Repository;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Predicate;

public final class ExecutableContextResolver {

    private static final ExecutableContext DEFAULT_CONTEXT = new DefaultExecutableContext();
    private static final ExecutableContextResolver DEFAULTS = new ExecutableContextResolver();

    private final Map<TaskType, List<BiFunction<Task, LoadedDomainObjects, ExecutableContext>>> providersByType;
    private final EntityType1Repository entityType1Repository;
    private final EntityType2Repository entityType2Repository;
    private final EntityType3Repository entityType3Repository;
    private final EntityType4Repository entityType4Repository;

    private ExecutableContextResolver() {
        this.providersByType = Map.of();
        this.entityType1Repository = null;
        this.entityType2Repository = null;
        this.entityType3Repository = null;
        this.entityType4Repository = null;
    }

    private ExecutableContextResolver(
            Map<TaskType, List<BiFunction<Task, LoadedDomainObjects, ExecutableContext>>> providersByType,
            EntityType1Repository entityType1Repository,
            EntityType2Repository entityType2Repository,
            EntityType3Repository entityType3Repository,
            EntityType4Repository entityType4Repository
    ) {
        Map<TaskType, List<BiFunction<Task, LoadedDomainObjects, ExecutableContext>>> copied = new EnumMap<>(TaskType.class);
        for (Map.Entry<TaskType, List<BiFunction<Task, LoadedDomainObjects, ExecutableContext>>> entry : providersByType.entrySet()) {
            TaskType taskType = Objects.requireNonNull(entry.getKey(), "taskType must not be null");
            List<BiFunction<Task, LoadedDomainObjects, ExecutableContext>> providers = Objects.requireNonNull(
                    entry.getValue(),
                    "providers must not be null"
            );
            for (BiFunction<Task, LoadedDomainObjects, ExecutableContext> provider : providers) {
                Objects.requireNonNull(provider, "provider must not be null");
            }
            copied.put(taskType, List.copyOf(providers));
        }
        this.providersByType = Map.copyOf(copied);
        this.entityType1Repository = Objects.requireNonNull(entityType1Repository, "entityType1Repository must not be null");
        this.entityType2Repository = Objects.requireNonNull(entityType2Repository, "entityType2Repository must not be null");
        this.entityType3Repository = Objects.requireNonNull(entityType3Repository, "entityType3Repository must not be null");
        this.entityType4Repository = Objects.requireNonNull(entityType4Repository, "entityType4Repository must not be null");
    }

    public static ExecutableContextResolver defaults() {
        return DEFAULTS;
    }

    public static ExecutableContextResolver of(
            Map<TaskType, List<BiFunction<Task, LoadedDomainObjects, ExecutableContext>>> providersByType,
            EntityType1Repository entityType1Repository,
            EntityType2Repository entityType2Repository,
            EntityType3Repository entityType3Repository,
            EntityType4Repository entityType4Repository
    ) {
        Objects.requireNonNull(providersByType, "providersByType must not be null");
        return new ExecutableContextResolver(
                providersByType,
                entityType1Repository,
                entityType2Repository,
                entityType3Repository,
                entityType4Repository
        );
    }

    public ExecutableContext resolve(Task task) {
        Objects.requireNonNull(task, "task must not be null");
        List<BiFunction<Task, LoadedDomainObjects, ExecutableContext>> providers = providersByType.get(task.type());
        if (providers == null || providers.isEmpty()) {
            return DEFAULT_CONTEXT;
        }
        LoadedDomainObjects loadedDomainObjects = loadDomainObjects(task);
        if (providers.size() == 1) {
            return Objects.requireNonNull(
                    providers.getFirst().apply(task, loadedDomainObjects),
                    "resolved ExecutableContext must not be null"
            );
        }
        List<Predicate<Task>> predicates = providers.stream()
                .map(provider -> Objects.requireNonNull(
                        provider.apply(task, loadedDomainObjects),
                        "resolved ExecutableContext must not be null"
                ))
                .flatMap(context -> context.predicates().stream())
                .toList();
        return new DefaultExecutableContext(loadedDomainObjects, predicates);
    }

    private LoadedDomainObjects loadDomainObjects(Task task) {
        List<LinkedEntity> linkedEntities = task.linkedEntities().entities();
        if (linkedEntities.isEmpty()) {
            return LoadedDomainObjects.empty();
        }
        List<DomainObject> loaded = new ArrayList<>();
        for (LinkedEntity linkedEntity : linkedEntities) {
            findByLinkedEntity(linkedEntity).ifPresent(loaded::add);
        }
        return LoadedDomainObjects.of(loaded);
    }

    private Optional<? extends DomainObject> findByLinkedEntity(LinkedEntity linkedEntity) {
        return switch (linkedEntity.typeName()) {
            case ENTITY_TYPE_1 -> entityType1Repository.findById(linkedEntity.entityId());
            case ENTITY_TYPE_2 -> entityType2Repository.findById(linkedEntity.entityId());
            case ENTITY_TYPE_3 -> entityType3Repository.findById(linkedEntity.entityId());
            case ENTITY_TYPE_4 -> entityType4Repository.findById(linkedEntity.entityId());
        };
    }
}
