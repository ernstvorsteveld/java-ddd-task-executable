package com.sternitc.task.adapters.out.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.ReplaceOptions;
import com.sternitc.task.domain.model.CommandExecutionStatus;
import com.sternitc.task.domain.model.CommandId;
import com.sternitc.task.domain.model.CommandPayload;
import com.sternitc.task.domain.model.EntityId;
import com.sternitc.task.domain.model.EntityTypeName;
import com.sternitc.task.domain.model.LinkedEntities;
import com.sternitc.task.domain.model.LinkedEntity;
import com.sternitc.task.domain.model.PlanningWindow;
import com.sternitc.task.domain.model.ProcessName;
import com.sternitc.task.domain.model.RoleName;
import com.sternitc.task.domain.model.StoredCommand;
import com.sternitc.task.domain.model.TaskCommandPayload;
import com.sternitc.task.domain.model.TaskExecutor;
import com.sternitc.task.domain.model.TaskId;
import com.sternitc.task.domain.model.TaskStatus;
import com.sternitc.task.domain.model.TaskType;
import com.sternitc.task.domain.model.UserId;
import com.sternitc.task.ports.out.CommandRepository;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bson.Document;

public final class MongoCommandRepository implements CommandRepository {

    private static final String COLLECTION_NAME = "commands";
    private static final String PAYLOAD_PLAN_TASK = "PLAN_TASK";
    private static final String PAYLOAD_UPDATE_TASK = "UPDATE_TASK";
    private static final String EXECUTOR_USER = "USER";
    private static final String EXECUTOR_ROLE = "ROLE";

    private final MongoCollection<Document> collection;

    public MongoCommandRepository(MongoDatabase database) {
        Objects.requireNonNull(database, "database must not be null");
        this.collection = database.getCollection(COLLECTION_NAME);
    }

    @Override
    public StoredCommand save(StoredCommand command) {
        Objects.requireNonNull(command, "command must not be null");
        Document document = toDocument(command);
        collection.replaceOne(
                Filters.eq("_id", command.id().value().toString()),
                document,
                new ReplaceOptions().upsert(true)
        );
        return command;
    }

    @Override
    public Optional<StoredCommand> findById(CommandId commandId) {
        Objects.requireNonNull(commandId, "commandId must not be null");
        Document found = collection.find(Filters.eq("_id", commandId.value().toString())).first();
        return Optional.ofNullable(found).map(MongoCommandRepository::toDomain);
    }

    private static Document toDocument(StoredCommand command) {
        var doc = new Document("_id", command.id().value().toString())
                .append("timestamp", command.timestamp().toString())
                .append("status", command.status().name());
        command.failureReason().ifPresent(reason -> doc.append("failureReason", reason));

        if (command.payload() instanceof TaskCommandPayload taskPayload) {
            appendTaskPayload(doc, taskPayload);
        } else {
            throw new IllegalStateException("Unsupported CommandPayload: " + command.payload().getClass().getName());
        }
        return doc;
    }

    private static void appendTaskPayload(Document doc, TaskCommandPayload taskPayload) {
        switch (taskPayload) {
            case TaskCommandPayload.PlanTask(var type, var owner, var window, var executor, var linkedEntities) -> {
                doc.append("payloadType", PAYLOAD_PLAN_TASK);
                doc.append("payload", new Document("type", type.name())
                        .append("owner", owner.name())
                        .append("startBefore", window.startBefore().toString())
                        .append("finishBefore", window.finishBefore().toString())
                        .append("executor", toExecutorDocument(executor))
                        .append("linkedEntities", toLinkedEntityDocuments(linkedEntities)));
            }
            case TaskCommandPayload.UpdateTask(var taskId, var status, var window, var executor) -> {
                doc.append("payloadType", PAYLOAD_UPDATE_TASK);
                doc.append("payload", new Document("taskId", taskId.value().toString())
                        .append("status", status.name())
                        .append("startBefore", window.startBefore().toString())
                        .append("finishBefore", window.finishBefore().toString())
                        .append("executor", toExecutorDocument(executor)));
            }
        }
    }

    private static List<Document> toLinkedEntityDocuments(LinkedEntities linkedEntities) {
        return linkedEntities.entities().stream()
                .map(entity -> new Document("typeName", entity.typeName().name())
                        .append("entityId", entity.entityId().value()))
                .toList();
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

    private static Document toExecutorDocument(TaskExecutor executor) {
        return switch (executor) {
            case TaskExecutor.User(var userId) -> new Document("type", EXECUTOR_USER).append("id", userId.value());
            case TaskExecutor.Role(var roleName) -> new Document("type", EXECUTOR_ROLE).append("id", roleName.value());
        };
    }

    private static StoredCommand toDomain(Document doc) {
        var id = new CommandId(UUID.fromString(doc.getString("_id")));
        Instant timestamp = Instant.parse(doc.getString("timestamp"));
        CommandExecutionStatus status = CommandExecutionStatus.valueOf(doc.getString("status"));
        String failureReason = doc.getString("failureReason");
        String payloadType = doc.getString("payloadType");
        Document payloadDoc = doc.get("payload", Document.class);
        CommandPayload payload = toPayload(payloadType, payloadDoc);
        return StoredCommand.reconstitute(id, timestamp, payload, status, failureReason);
    }

    private static CommandPayload toPayload(String payloadType, Document payloadDoc) {
        var window = new PlanningWindow(
                Instant.parse(payloadDoc.getString("startBefore")),
                Instant.parse(payloadDoc.getString("finishBefore"))
        );
        TaskExecutor executor = toExecutor(payloadDoc.get("executor", Document.class));
        return switch (payloadType) {
            case PAYLOAD_PLAN_TASK -> new TaskCommandPayload.PlanTask(
                    TaskType.valueOf(payloadDoc.getString("type")),
                    ProcessName.valueOf(payloadDoc.getString("owner")),
                    window,
                    executor,
                    toLinkedEntities(payloadDoc.getList("linkedEntities", Document.class))
            );
            case PAYLOAD_UPDATE_TASK -> new TaskCommandPayload.UpdateTask(
                    new TaskId(UUID.fromString(payloadDoc.getString("taskId"))),
                    TaskStatus.valueOf(payloadDoc.getString("status")),
                    window,
                    executor
            );
            default -> throw new IllegalStateException("Unsupported command payloadType: " + payloadType);
        };
    }

    private static TaskExecutor toExecutor(Document doc) {
        String type = doc.getString("type");
        String id = doc.getString("id");
        return switch (type) {
            case EXECUTOR_USER -> new TaskExecutor.User(new UserId(id));
            case EXECUTOR_ROLE -> new TaskExecutor.Role(new RoleName(id));
            default -> throw new IllegalStateException("Unsupported executor type: " + type);
        };
    }
}
