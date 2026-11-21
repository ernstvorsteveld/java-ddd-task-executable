package com.sternitc.task.adapters.out.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.mongodb.client.model.Sorts;
import com.sternitc.task.domain.model.EntityId;
import com.sternitc.task.domain.model.EntityTypeName;
import com.sternitc.task.domain.model.InvalidPageRequestException;
import com.sternitc.task.domain.model.LinkedEntities;
import com.sternitc.task.domain.model.LinkedEntity;
import com.sternitc.task.domain.model.PlanningWindow;
import com.sternitc.task.domain.model.ProcessName;
import com.sternitc.task.domain.model.RoleName;
import com.sternitc.task.domain.model.Task;
import com.sternitc.task.domain.model.TaskExecutor;
import com.sternitc.task.domain.model.TaskId;
import com.sternitc.task.domain.model.TaskSearchCriteria;
import com.sternitc.task.domain.model.TaskStatus;
import com.sternitc.task.domain.model.TaskType;
import com.sternitc.task.domain.model.TaskVersion;
import com.sternitc.task.domain.model.UserId;
import com.sternitc.task.ports.out.TaskRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantLock;
import org.bson.Document;
import org.bson.conversions.Bson;

public final class MongoTaskRepository implements TaskRepository {

    private static final String COLLECTION_NAME = "tasks";
    private static final String EXECUTOR_USER = "USER";
    private static final String EXECUTOR_ROLE = "ROLE";

    private final MongoCollection<Document> collection;
    private final AtomicLong sequence = new AtomicLong();
    private final ReentrantLock writeLock = new ReentrantLock();

    public MongoTaskRepository(MongoDatabase database) {
        Objects.requireNonNull(database, "database must not be null");
        this.collection = database.getCollection(COLLECTION_NAME);
    }

    @Override
    public Task save(Task task) {
        Objects.requireNonNull(task, "task must not be null");
        String id = task.id().value().toString();
        writeLock.lock();
        try {
            Document existing = collection.find(Filters.eq("_id", id)).first();
            long createdOrder = existing != null
                    ? existing.getLong("createdOrder")
                    : sequence.incrementAndGet();
            collection.replaceOne(
                    Filters.eq("_id", id),
                    toDocument(task, createdOrder),
                    new ReplaceOptions().upsert(true)
            );
            return task;
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public Optional<Task> findById(TaskId taskId) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Document found = collection.find(Filters.eq("_id", taskId.value().toString())).first();
        return Optional.ofNullable(found).map(MongoTaskRepository::toDomain);
    }

    @Override
    public List<Task> findByExecutor(TaskExecutor executor, int pageNumber, int pageSize) {
        Objects.requireNonNull(executor, "executor must not be null");
        return search(TaskSearchCriteria.empty().withExecutor(executor), pageNumber, pageSize);
    }

    @Override
    public List<Task> search(TaskSearchCriteria criteria, int pageNumber, int pageSize) {
        Objects.requireNonNull(criteria, "criteria must not be null");
        if (pageNumber < 0) {
            throw new InvalidPageRequestException("pageNumber must not be negative");
        }
        if (pageSize <= 0) {
            throw new InvalidPageRequestException("pageSize must be greater than 0");
        }
        long offset = (long) pageNumber * pageSize;
        if (offset > Integer.MAX_VALUE) {
            return List.of();
        }

        Bson filter = buildFilter(criteria);
        List<Task> results = new ArrayList<>();
        for (Document doc : collection.find(filter)
                .sort(Sorts.ascending("createdOrder"))
                .skip((int) offset)
                .limit(pageSize)) {
            results.add(toDomain(doc));
        }
        return List.copyOf(results);
    }

    private static Bson buildFilter(TaskSearchCriteria criteria) {
        List<Bson> filters = new ArrayList<>();
        criteria.executor().ifPresent(executor -> {
            switch (executor) {
                case TaskExecutor.User(var userId) -> {
                    filters.add(Filters.eq("executorType", EXECUTOR_USER));
                    filters.add(Filters.eq("executorId", userId.value()));
                }
                case TaskExecutor.Role(var roleName) -> {
                    filters.add(Filters.eq("executorType", EXECUTOR_ROLE));
                    filters.add(Filters.eq("executorId", roleName.value()));
                }
            }
        });
        criteria.status().ifPresent(status -> filters.add(Filters.eq("status", status.name())));
        criteria.type().ifPresent(type -> filters.add(Filters.eq("type", type.name())));
        criteria.owner().ifPresent(owner -> filters.add(Filters.eq("owner", owner.name())));
        return filters.isEmpty() ? new Document() : Filters.and(filters);
    }

    private static Document toDocument(Task task, long createdOrder) {
        String executorType;
        String executorId;
        switch (task.executor()) {
            case TaskExecutor.User(var userId) -> {
                executorType = EXECUTOR_USER;
                executorId = userId.value();
            }
            case TaskExecutor.Role(var roleName) -> {
                executorType = EXECUTOR_ROLE;
                executorId = roleName.value();
            }
        }
        List<Document> linkedDocs = task.linkedEntities().entities().stream()
                .map(entity -> new Document("typeName", entity.typeName().name())
                        .append("entityId", entity.entityId().value()))
                .toList();
        return new Document("_id", task.id().value().toString())
                .append("type", task.type().name())
                .append("version", task.version().value())
                .append("status", task.status().name())
                .append("owner", task.owner().name())
                .append("startBefore", task.planningWindow().startBefore().toString())
                .append("finishBefore", task.planningWindow().finishBefore().toString())
                .append("executorType", executorType)
                .append("executorId", executorId)
                .append("linkedEntities", linkedDocs)
                .append("createdOrder", createdOrder);
    }

    private static Task toDomain(Document doc) {
        var id = new TaskId(UUID.fromString(doc.getString("_id")));
        TaskType type = TaskType.valueOf(doc.getString("type"));
        var version = new TaskVersion(doc.getInteger("version"));
        TaskStatus status = TaskStatus.valueOf(doc.getString("status"));
        ProcessName owner = ProcessName.valueOf(doc.getString("owner"));
        var window = new PlanningWindow(
                Instant.parse(doc.getString("startBefore")),
                Instant.parse(doc.getString("finishBefore"))
        );
        TaskExecutor executor = switch (doc.getString("executorType")) {
            case EXECUTOR_USER -> new TaskExecutor.User(new UserId(doc.getString("executorId")));
            case EXECUTOR_ROLE -> new TaskExecutor.Role(new RoleName(doc.getString("executorId")));
            default -> throw new IllegalStateException("Unsupported executorType: " + doc.getString("executorType"));
        };
        LinkedEntities linkedEntities = toLinkedEntities(doc.getList("linkedEntities", Document.class));
        return Task.restore(id, type, version, status, owner, window, executor, linkedEntities);
    }

    private static LinkedEntities toLinkedEntities(List<Document> docs) {
        if (docs == null || docs.isEmpty()) {
            return LinkedEntities.empty();
        }
        List<LinkedEntity> entities = docs.stream()
                .map(d -> new LinkedEntity(
                        EntityTypeName.valueOf(d.getString("typeName")),
                        new EntityId(d.getString("entityId"))
                ))
                .toList();
        return new LinkedEntities(entities);
    }
}
