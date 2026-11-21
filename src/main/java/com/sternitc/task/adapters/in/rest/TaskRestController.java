package com.sternitc.task.adapters.in.rest;

import com.sternitc.task.api.TasksApi;
import com.sternitc.task.api.model.CommandAcceptedResponsePayload;
import com.sternitc.task.api.model.CreateTaskRequestPayload;
import com.sternitc.task.api.model.EntityTypeNameDto;
import com.sternitc.task.api.model.ExecutorTypeDto;
import com.sternitc.task.api.model.LinkedEntityDto;
import com.sternitc.task.api.model.ProcessNameDto;
import com.sternitc.task.api.model.TaskExecutorDto;
import com.sternitc.task.api.model.TaskResponsePayload;
import com.sternitc.task.api.model.TaskStatusDto;
import com.sternitc.task.api.model.TaskTypeDto;
import com.sternitc.task.api.model.TasksResponsePayload;
import com.sternitc.task.api.model.UpdateTaskRequestPayload;
import com.sternitc.task.domain.model.CommandReceipt;
import com.sternitc.task.domain.model.EntityId;
import com.sternitc.task.domain.model.EntityTypeName;
import com.sternitc.task.domain.model.InvalidLinkedEntityException;
import com.sternitc.task.domain.model.InvalidPlanningWindowException;
import com.sternitc.task.domain.model.InvalidProcessNameException;
import com.sternitc.task.domain.model.InvalidTaskExecutorException;
import com.sternitc.task.domain.model.InvalidTaskStatusException;
import com.sternitc.task.domain.model.InvalidTaskTypeException;
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
import com.sternitc.task.domain.model.UserId;
import com.sternitc.task.ports.in.PlanTaskCommand;
import com.sternitc.task.ports.in.PlanTaskUseCase;
import com.sternitc.task.ports.in.SearchTasksQuery;
import com.sternitc.task.ports.in.TaskQuery;
import com.sternitc.task.ports.in.UpdateTaskCommand;
import com.sternitc.task.ports.in.UpdateTaskUseCase;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public final class TaskRestController implements TasksApi {

    private final PlanTaskUseCase planTaskUseCase;
    private final UpdateTaskUseCase updateTaskUseCase;
    private final TaskQuery taskQuery;

    public TaskRestController(
            PlanTaskUseCase planTaskUseCase,
            UpdateTaskUseCase updateTaskUseCase,
            TaskQuery taskQuery
    ) {
        this.planTaskUseCase = Objects.requireNonNull(planTaskUseCase, "planTaskUseCase must not be null");
        this.updateTaskUseCase = Objects.requireNonNull(updateTaskUseCase, "updateTaskUseCase must not be null");
        this.taskQuery = Objects.requireNonNull(taskQuery, "taskQuery must not be null");
    }

    @Override
    public ResponseEntity<CommandAcceptedResponsePayload> createTask(CreateTaskRequestPayload createTaskRequestPayload) {
        Objects.requireNonNull(createTaskRequestPayload, "createTaskRequestPayload must not be null");
        var command = new PlanTaskCommand(
                toDomain(createTaskRequestPayload.getType()),
                toDomain(createTaskRequestPayload.getOwner()),
                toPlanningWindow(createTaskRequestPayload.getStartBefore(), createTaskRequestPayload.getFinishBefore()),
                toDomain(createTaskRequestPayload.getExecutor()),
                toDomain(createTaskRequestPayload.getLinkedEntities())
        );
        CommandReceipt receipt = planTaskUseCase.planTask(command);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(toAcceptedPayload(receipt));
    }

    @Override
    public ResponseEntity<CommandAcceptedResponsePayload> updateTask(
            UUID taskId,
            UpdateTaskRequestPayload updateTaskRequestPayload
    ) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        Objects.requireNonNull(updateTaskRequestPayload, "updateTaskRequestPayload must not be null");
        var command = new UpdateTaskCommand(
                new TaskId(taskId),
                toDomain(updateTaskRequestPayload.getStatus()),
                toPlanningWindow(updateTaskRequestPayload.getStartBefore(), updateTaskRequestPayload.getFinishBefore()),
                toDomain(updateTaskRequestPayload.getExecutor())
        );
        CommandReceipt receipt = updateTaskUseCase.updateTask(command);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(toAcceptedPayload(receipt));
    }

    @Override
    public ResponseEntity<TasksResponsePayload> searchTasks(
            ExecutorTypeDto executorType,
            String executorId,
            TaskStatusDto status,
            TaskTypeDto type,
            ProcessNameDto owner,
            Integer pageNumber,
            Integer pageSize
    ) {
        int resolvedPageNumber = pageNumber != null ? pageNumber : SearchTasksQuery.DEFAULT_PAGE_NUMBER;
        int resolvedPageSize = pageSize != null ? pageSize : SearchTasksQuery.DEFAULT_PAGE_SIZE;

        TaskSearchCriteria criteria = buildCriteria(executorType, executorId, status, type, owner);
        List<TaskResponsePayload> taskPayloads = taskQuery
                .search(new SearchTasksQuery(criteria, resolvedPageNumber, resolvedPageSize))
                .stream()
                .map(TaskRestController::toPayload)
                .toList();

        var response = new TasksResponsePayload(taskPayloads, taskPayloads.size(), resolvedPageNumber, resolvedPageSize)
                .executorType(executorType)
                .executorId(executorId)
                .status(status)
                .type(type)
                .owner(owner);
        return ResponseEntity.ok(response);
    }

    private static CommandAcceptedResponsePayload toAcceptedPayload(CommandReceipt receipt) {
        return new CommandAcceptedResponsePayload(
                receipt.commandId().value(),
                toOffsetDateTime(receipt.timestamp())
        );
    }

    private static TaskSearchCriteria buildCriteria(
            ExecutorTypeDto executorType,
            String executorId,
            TaskStatusDto status,
            TaskTypeDto type,
            ProcessNameDto owner
    ) {
        TaskSearchCriteria criteria = TaskSearchCriteria.empty();
        if (executorType != null || executorId != null) {
            if (executorType == null || executorId == null) {
                throw new InvalidTaskExecutorException("Both executorType and executorId must be provided together");
            }
            criteria = criteria.withExecutor(toDomain(executorType, executorId));
        }
        if (status != null) {
            criteria = criteria.withStatus(toDomain(status));
        }
        if (type != null) {
            criteria = criteria.withType(toDomain(type));
        }
        if (owner != null) {
            criteria = criteria.withOwner(toDomain(owner));
        }
        return criteria;
    }

    private static PlanningWindow toPlanningWindow(OffsetDateTime startBefore, OffsetDateTime finishBefore) {
        if (startBefore == null || finishBefore == null) {
            throw new InvalidPlanningWindowException("startBefore and finishBefore must not be null");
        }
        return new PlanningWindow(startBefore.toInstant(), finishBefore.toInstant());
    }

    private static LinkedEntities toDomain(List<LinkedEntityDto> dtos) {
        if (dtos == null || dtos.isEmpty()) {
            return LinkedEntities.empty();
        }
        List<LinkedEntity> entities = dtos.stream()
                .map(dto -> {
                    if (dto == null) {
                        throw new InvalidLinkedEntityException("linked entity element must not be null");
                    }
                    return new LinkedEntity(toDomain(dto.getTypeName()), new EntityId(dto.getEntityId()));
                })
                .toList();
        return new LinkedEntities(entities);
    }

    private static EntityTypeName toDomain(EntityTypeNameDto dto) {
        if (dto == null) {
            throw new InvalidLinkedEntityException("typeName must not be null");
        }
        return switch (dto) {
            case ENTITY_TYPE_1 -> EntityTypeName.ENTITY_TYPE_1;
            case ENTITY_TYPE_2 -> EntityTypeName.ENTITY_TYPE_2;
            case ENTITY_TYPE_3 -> EntityTypeName.ENTITY_TYPE_3;
            case ENTITY_TYPE_4 -> EntityTypeName.ENTITY_TYPE_4;
        };
    }

    private static TaskType toDomain(TaskTypeDto dto) {
        if (dto == null) {
            throw new InvalidTaskTypeException("type must not be null");
        }
        return switch (dto) {
            case DO_AANVRAAG -> TaskType.DO_AANVRAAG;
            case DO_OPDRACHT -> TaskType.DO_OPDRACHT;
            case DO_OFFERTE -> TaskType.DO_OFFERTE;
            case DO_CONTRACT -> TaskType.DO_CONTRACT;
        };
    }

    private static ProcessName toDomain(ProcessNameDto dto) {
        if (dto == null) {
            throw new InvalidProcessNameException("owner must not be null");
        }
        return switch (dto) {
            case PS_IE -> ProcessName.PS_IE;
            case PS_UE -> ProcessName.PS_UE;
            case PO_BO -> ProcessName.PO_BO;
            case PO_VG -> ProcessName.PO_VG;
            case PO_UG -> ProcessName.PO_UG;
        };
    }

    private static TaskStatus toDomain(TaskStatusDto dto) {
        if (dto == null) {
            throw new InvalidTaskStatusException("status must not be null");
        }
        return switch (dto) {
            case CREATED -> TaskStatus.CREATED;
            case IN_PROGRESS -> TaskStatus.IN_PROGRESS;
            case PAUSED -> TaskStatus.PAUSED;
            case CANCELLED -> TaskStatus.CANCELLED;
            case DONE -> TaskStatus.DONE;
        };
    }

    private static TaskExecutor toDomain(TaskExecutorDto dto) {
        if (dto == null || dto.getType() == null) {
            throw new InvalidTaskExecutorException("executor and executor.type must not be null");
        }
        return toDomain(dto.getType(), dto.getId());
    }

    private static TaskExecutor toDomain(ExecutorTypeDto type, String id) {
        return switch (type) {
            case USER -> new TaskExecutor.User(new UserId(id));
            case ROLE -> new TaskExecutor.Role(new RoleName(id));
        };
    }

    private static TaskResponsePayload toPayload(Task task) {
        return new TaskResponsePayload(
                task.id().value(),
                toDto(task.type()),
                task.version().value(),
                toDto(task.status()),
                toDto(task.owner()),
                toOffsetDateTime(task.planningWindow().startBefore()),
                toOffsetDateTime(task.planningWindow().finishBefore()),
                toDto(task.executor()),
                toDto(task.linkedEntities())
        );
    }

    private static List<LinkedEntityDto> toDto(LinkedEntities linkedEntities) {
        return linkedEntities.entities().stream()
                .map(entity -> new LinkedEntityDto(toDto(entity.typeName()), entity.entityId().value()))
                .toList();
    }

    private static EntityTypeNameDto toDto(EntityTypeName typeName) {
        return switch (typeName) {
            case ENTITY_TYPE_1 -> EntityTypeNameDto.ENTITY_TYPE_1;
            case ENTITY_TYPE_2 -> EntityTypeNameDto.ENTITY_TYPE_2;
            case ENTITY_TYPE_3 -> EntityTypeNameDto.ENTITY_TYPE_3;
            case ENTITY_TYPE_4 -> EntityTypeNameDto.ENTITY_TYPE_4;
        };
    }

    private static OffsetDateTime toOffsetDateTime(Instant instant) {
        return instant.atOffset(ZoneOffset.UTC);
    }

    private static TaskTypeDto toDto(TaskType type) {
        return switch (type) {
            case DO_AANVRAAG -> TaskTypeDto.DO_AANVRAAG;
            case DO_OPDRACHT -> TaskTypeDto.DO_OPDRACHT;
            case DO_OFFERTE -> TaskTypeDto.DO_OFFERTE;
            case DO_CONTRACT -> TaskTypeDto.DO_CONTRACT;
        };
    }

    private static TaskStatusDto toDto(TaskStatus status) {
        return switch (status) {
            case CREATED -> TaskStatusDto.CREATED;
            case IN_PROGRESS -> TaskStatusDto.IN_PROGRESS;
            case PAUSED -> TaskStatusDto.PAUSED;
            case CANCELLED -> TaskStatusDto.CANCELLED;
            case DONE -> TaskStatusDto.DONE;
        };
    }

    private static ProcessNameDto toDto(ProcessName owner) {
        return switch (owner) {
            case PS_IE -> ProcessNameDto.PS_IE;
            case PS_UE -> ProcessNameDto.PS_UE;
            case PO_BO -> ProcessNameDto.PO_BO;
            case PO_VG -> ProcessNameDto.PO_VG;
            case PO_UG -> ProcessNameDto.PO_UG;
        };
    }

    private static TaskExecutorDto toDto(TaskExecutor executor) {
        return switch (executor) {
            case TaskExecutor.User(var userId) -> new TaskExecutorDto(ExecutorTypeDto.USER, userId.value());
            case TaskExecutor.Role(var roleName) -> new TaskExecutorDto(ExecutorTypeDto.ROLE, roleName.value());
        };
    }
}
