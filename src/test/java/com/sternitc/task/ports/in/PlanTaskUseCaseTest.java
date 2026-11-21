package com.sternitc.task.ports.in;

import com.sternitc.task.domain.model.CommandExecutionStatus;
import com.sternitc.task.domain.model.CommandReceipt;
import com.sternitc.task.domain.model.EntityId;
import com.sternitc.task.domain.model.EntityTypeName;
import com.sternitc.task.domain.model.InvalidLinkedEntityException;
import com.sternitc.task.domain.model.InvalidPlanningWindowException;
import com.sternitc.task.domain.model.InvalidProcessNameException;
import com.sternitc.task.domain.model.InvalidTaskExecutorException;
import com.sternitc.task.domain.model.InvalidTaskTypeException;
import com.sternitc.task.domain.model.LinkedEntities;
import com.sternitc.task.domain.model.LinkedEntity;
import com.sternitc.task.domain.model.PlanningWindow;
import com.sternitc.task.domain.model.ProcessName;
import com.sternitc.task.domain.model.RoleName;
import com.sternitc.task.domain.model.Task;
import com.sternitc.task.domain.model.TaskExecutor;
import com.sternitc.task.domain.model.TaskSearchCriteria;
import com.sternitc.task.domain.model.TaskStatus;
import com.sternitc.task.domain.model.TaskType;
import com.sternitc.task.domain.model.TaskVersion;
import com.sternitc.task.domain.model.UserId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PlanTaskUseCase Inbound Port")
class PlanTaskUseCaseTest extends InboundPortTestSupport {

    @Nested
    @DisplayName("Requirement 1: Create Task")
    class CreateTaskSpec {

        @Test
        @DisplayName("1.1 creates and returns a task with TaskType, initial version 1, initial status CREATED, ProcessName owner, PlanningWindow, and User executor")
        void createsTaskAssignedToUser() {
            Task created = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice"))
            ));

            assertThat(created.id()).isNotNull();
            assertThat(created.id().value()).isNotNull();
            assertThat(created.type()).isEqualTo(TaskType.DO_AANVRAAG);
            assertThat(created.version()).isEqualTo(new TaskVersion(1));
            assertThat(created.status()).isEqualTo(TaskStatus.CREATED);
            assertThat(created.owner()).isEqualTo(ProcessName.PS_IE);
            assertThat(created.planningWindow()).isEqualTo(new PlanningWindow(START_BEFORE, FINISH_BEFORE));
            assertThat(created.executor()).isEqualTo(new TaskExecutor.User(new UserId("alice")));
        }

        @Test
        @DisplayName("1.1 creates a task assigned to a Role when startBefore equals finishBefore")
        void createsTaskAssignedToRoleWithEqualTimestamps() {
            Task created = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_CONTRACT,
                    ProcessName.PO_UG,
                    new PlanningWindow(START_BEFORE, START_BEFORE),
                    new TaskExecutor.Role(new RoleName("FINANCE_APPROVER"))
            ));

            assertThat(created.id()).isNotNull();
            assertThat(created.type()).isEqualTo(TaskType.DO_CONTRACT);
            assertThat(created.version()).isEqualTo(new TaskVersion(1));
            assertThat(created.owner()).isEqualTo(ProcessName.PO_UG);
            assertThat(created.executor()).isEqualTo(new TaskExecutor.Role(new RoleName("FINANCE_APPROVER")));
            assertThat(created.planningWindow()).isEqualTo(new PlanningWindow(START_BEFORE, START_BEFORE));
        }

        @Test
        @DisplayName("1.1 assigns a unique TaskId to each created task")
        void assignsDistinctTaskIds() {
            var command = new PlanTaskCommand(
                    TaskType.DO_OPDRACHT,
                    ProcessName.PS_UE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice"))
            );

            Task first = planAndAwaitTask(command);
            Task second = planAndAwaitTask(command);

            assertThat(first.id()).isNotEqualTo(second.id());
        }

        @Test
        @DisplayName("1.2 rejects missing startBefore, missing finishBefore, or null PlanningWindow")
        void rejectsMissingPlanningTimestamps() {
            assertThatThrownBy(() -> planTaskUseCase.planTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(null, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice"))
            ))).isInstanceOf(InvalidPlanningWindowException.class);

            assertThatThrownBy(() -> planTaskUseCase.planTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, null),
                    new TaskExecutor.User(new UserId("alice"))
            ))).isInstanceOf(InvalidPlanningWindowException.class);

            assertThatThrownBy(() -> planTaskUseCase.planTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    null,
                    new TaskExecutor.User(new UserId("alice"))
            ))).isInstanceOf(InvalidPlanningWindowException.class);
        }

        @Test
        @DisplayName("1.2 rejects startBefore after finishBefore without persisting task")
        void rejectsStartBeforeAfterFinishBefore() {
            assertThatThrownBy(() -> planTaskUseCase.planTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(FINISH_BEFORE, START_BEFORE),
                    new TaskExecutor.User(new UserId("alice"))
            ))).isInstanceOf(InvalidPlanningWindowException.class);

            assertThat(taskQuery.search(SearchTasksQuery.of(TaskSearchCriteria.empty()))).isEmpty();
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("1.3 rejects blank or null UserId and RoleName")
        void rejectsBlankOrNullExecutorIdentifiers(String invalidValue) {
            assertThatThrownBy(() -> planTaskUseCase.planTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId(invalidValue))
            ))).isInstanceOf(InvalidTaskExecutorException.class);

            assertThatThrownBy(() -> planTaskUseCase.planTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.Role(new RoleName(invalidValue))
            ))).isInstanceOf(InvalidTaskExecutorException.class);
        }

        @Test
        @DisplayName("1.3 rejects null TaskExecutor or null UserId/RoleName wrapper")
        void rejectsNullExecutor() {
            assertThatThrownBy(() -> planTaskUseCase.planTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    null
            ))).isInstanceOf(InvalidTaskExecutorException.class);

            assertThatThrownBy(() -> planTaskUseCase.planTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(null)
            ))).isInstanceOf(InvalidTaskExecutorException.class);

            assertThatThrownBy(() -> planTaskUseCase.planTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.Role(null)
            ))).isInstanceOf(InvalidTaskExecutorException.class);
        }

        @Test
        @DisplayName("1.4 allows exactly the controlled TaskType values and rejects missing TaskType")
        void enforcesControlledTaskType() {
            assertThat(TaskType.values()).containsExactly(
                    TaskType.DO_AANVRAAG,
                    TaskType.DO_OPDRACHT,
                    TaskType.DO_OFFERTE,
                    TaskType.DO_CONTRACT
            );

            assertThatThrownBy(() -> planTaskUseCase.planTask(new PlanTaskCommand(
                    null,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice"))
            ))).isInstanceOf(InvalidTaskTypeException.class);
        }

        @Test
        @DisplayName("1.5 allows exactly the controlled ProcessName owner values and rejects missing ProcessName")
        void enforcesControlledProcessNameOwner() {
            assertThat(ProcessName.values()).containsExactly(
                    ProcessName.PS_IE,
                    ProcessName.PS_UE,
                    ProcessName.PO_BO,
                    ProcessName.PO_VG,
                    ProcessName.PO_UG
            );

            assertThatThrownBy(() -> planTaskUseCase.planTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    null,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice"))
            ))).isInstanceOf(InvalidProcessNameException.class);
        }

        @Test
        @DisplayName("1.6 initializes TaskVersion to 1 on create and increments by 1 on reconstitution via update")
        void initializesVersionToOneAndIncrementsOnReconstitute() {
            var alice = new TaskExecutor.User(new UserId("alice"));
            Task created = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_OFFERTE,
                    ProcessName.PO_BO,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    alice
            ));

            assertThat(created.version()).isEqualTo(new TaskVersion(1));

            CommandReceipt updateReceipt = updateTaskUseCase.updateTask(new UpdateTaskCommand(
                    created.id(),
                    TaskStatus.IN_PROGRESS,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    alice
            ));
            awaitCommandStatus(updateReceipt, CommandExecutionStatus.COMPLETED);

            Task reconstituted = taskQuery.findById(created.id()).orElseThrow();
            assertThat(reconstituted.version()).isEqualTo(new TaskVersion(2));
        }

        @Test
        @DisplayName("1.7 supports CREATED, IN_PROGRESS, PAUSED, CANCELLED, DONE and initializes status to CREATED")
        void initializesStatusToCreated() {
            assertThat(TaskStatus.values()).containsExactly(
                    TaskStatus.CREATED,
                    TaskStatus.IN_PROGRESS,
                    TaskStatus.PAUSED,
                    TaskStatus.CANCELLED,
                    TaskStatus.DONE
            );

            Task created = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice"))
            ));

            assertThat(created.status()).isEqualTo(TaskStatus.CREATED);
        }

        @Test
        @DisplayName("1.8 allows empty or populated LinkedEntities, persists them, and preserves them across updates")
        void createsTaskWithLinkedEntitiesAndPreservesAcrossUpdate() {
            assertThat(EntityTypeName.values()).containsExactly(
                    EntityTypeName.ENTITY_TYPE_1,
                    EntityTypeName.ENTITY_TYPE_2,
                    EntityTypeName.ENTITY_TYPE_3,
                    EntityTypeName.ENTITY_TYPE_4
            );

            Task withEmpty = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice")),
                    LinkedEntities.empty()
            ));
            assertThat(withEmpty.linkedEntities().entities()).isEmpty();

            var linked = new LinkedEntities(List.of(
                    new LinkedEntity(EntityTypeName.ENTITY_TYPE_1, new EntityId("ent-100")),
                    new LinkedEntity(EntityTypeName.ENTITY_TYPE_4, new EntityId("ent-400"))
            ));
            Task withLinked = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_CONTRACT,
                    ProcessName.PO_VG,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice")),
                    linked
            ));
            assertThat(withLinked.linkedEntities()).isEqualTo(linked);

            CommandReceipt updateReceipt = updateTaskUseCase.updateTask(new UpdateTaskCommand(
                    withLinked.id(),
                    TaskStatus.IN_PROGRESS,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice"))
            ));
            awaitCommandStatus(updateReceipt, CommandExecutionStatus.COMPLETED);
            Task updated = taskQuery.findById(withLinked.id()).orElseThrow();
            assertThat(updated.linkedEntities()).isEqualTo(linked);
        }

        @ParameterizedTest
        @NullAndEmptySource
        @ValueSource(strings = {"   "})
        @DisplayName("1.8 rejects blank or null EntityId in LinkedEntity")
        void rejectsInvalidEntityId(String invalidEntityId) {
            assertThatThrownBy(() -> planTaskUseCase.planTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice")),
                    new LinkedEntities(List.of(new LinkedEntity(EntityTypeName.ENTITY_TYPE_1, new EntityId(invalidEntityId))))
            ))).isInstanceOf(InvalidLinkedEntityException.class);
        }

        @Test
        @DisplayName("1.8 rejects null LinkedEntities, null list, null LinkedEntity element, or missing EntityTypeName/EntityId")
        void rejectsNullLinkedEntitiesOrFields() {
            assertThatThrownBy(() -> planTaskUseCase.planTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice")),
                    null
            ))).isInstanceOf(InvalidLinkedEntityException.class);

            assertThatThrownBy(() -> new LinkedEntities(null))
                    .isInstanceOf(InvalidLinkedEntityException.class);

            var listWithNull = new ArrayList<LinkedEntity>();
            listWithNull.add(null);
            assertThatThrownBy(() -> new LinkedEntities(listWithNull))
                    .isInstanceOf(InvalidLinkedEntityException.class);

            assertThatThrownBy(() -> new LinkedEntity(null, new EntityId("ent-1")))
                    .isInstanceOf(InvalidLinkedEntityException.class);

            assertThatThrownBy(() -> new LinkedEntity(EntityTypeName.ENTITY_TYPE_2, null))
                    .isInstanceOf(InvalidLinkedEntityException.class);
        }
    }
}
