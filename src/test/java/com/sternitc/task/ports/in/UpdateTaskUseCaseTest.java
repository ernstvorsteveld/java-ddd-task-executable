package com.sternitc.task.ports.in;

import com.sternitc.task.domain.model.CommandExecutionStatus;
import com.sternitc.task.domain.model.CommandReceipt;
import com.sternitc.task.domain.model.InvalidPlanningWindowException;
import com.sternitc.task.domain.model.InvalidTaskExecutorException;
import com.sternitc.task.domain.model.InvalidTaskStatusException;
import com.sternitc.task.domain.model.PlanningWindow;
import com.sternitc.task.domain.model.ProcessName;
import com.sternitc.task.domain.model.RoleName;
import com.sternitc.task.domain.model.StoredCommand;
import com.sternitc.task.domain.model.Task;
import com.sternitc.task.domain.model.TaskExecutor;
import com.sternitc.task.domain.model.TaskId;
import com.sternitc.task.domain.model.TaskStatus;
import com.sternitc.task.domain.model.TaskType;
import com.sternitc.task.domain.model.TaskVersion;
import com.sternitc.task.domain.model.UserId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("UpdateTaskUseCase Inbound Port")
class UpdateTaskUseCaseTest extends InboundPortTestSupport {

    @Nested
    @DisplayName("Requirement 2: Update Task")
    class UpdateTaskSpec {

        @Test
        @DisplayName("2.1 updates status, planningWindow, and executor asynchronously, increments version, and persists")
        void updatesExistingTask() {
            Task created = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice"))
            ));
            var newWindow = new PlanningWindow(
                    Instant.parse("2026-11-23T09:00:00Z"),
                    Instant.parse("2026-11-23T17:00:00Z")
            );
            var newExecutor = new TaskExecutor.Role(new RoleName("APPROVER"));

            CommandReceipt receipt = updateTaskUseCase.updateTask(new UpdateTaskCommand(
                    created.id(),
                    TaskStatus.IN_PROGRESS,
                    newWindow,
                    newExecutor
            ));
            awaitCommandStatus(receipt, CommandExecutionStatus.COMPLETED);

            Task persisted = taskQuery.findById(created.id()).orElseThrow();
            assertThat(persisted.id()).isEqualTo(created.id());
            assertThat(persisted.type()).isEqualTo(TaskType.DO_AANVRAAG);
            assertThat(persisted.owner()).isEqualTo(ProcessName.PS_IE);
            assertThat(persisted.version()).isEqualTo(new TaskVersion(2));
            assertThat(persisted.status()).isEqualTo(TaskStatus.IN_PROGRESS);
            assertThat(persisted.planningWindow()).isEqualTo(newWindow);
            assertThat(persisted.executor()).isEqualTo(newExecutor);
        }

        @Test
        @DisplayName("2.2 records failure on stored command when async update targets unknown TaskId")
        void recordsFailureOnAsyncUpdateForUnknownTaskId() {
            Task existing = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice"))
            ));

            CommandReceipt receipt = updateTaskUseCase.updateTask(new UpdateTaskCommand(
                    new TaskId(UUID.randomUUID()),
                    TaskStatus.IN_PROGRESS,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice"))
            ));

            StoredCommand failed = awaitCommandStatus(receipt, CommandExecutionStatus.FAILED);
            assertThat(failed.failureReason()).isPresent();
            assertThat(failed.failureReason().orElseThrow()).contains("Task not found");
            assertThat(taskQuery.findById(existing.id())).contains(existing);
        }

        @Test
        @DisplayName("2.3 rejects update when status, planningWindow, or executor is missing")
        void rejectsMissingUpdateFields() {
            Task created = planAndAwaitTask(new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice"))
            ));

            assertThatThrownBy(() -> updateTaskUseCase.updateTask(new UpdateTaskCommand(
                    created.id(),
                    null,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice"))
            ))).isInstanceOf(InvalidTaskStatusException.class);

            assertThatThrownBy(() -> updateTaskUseCase.updateTask(new UpdateTaskCommand(
                    created.id(),
                    TaskStatus.PAUSED,
                    null,
                    new TaskExecutor.User(new UserId("alice"))
            ))).isInstanceOf(InvalidPlanningWindowException.class);

            assertThatThrownBy(() -> updateTaskUseCase.updateTask(new UpdateTaskCommand(
                    created.id(),
                    TaskStatus.PAUSED,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    null
            ))).isInstanceOf(InvalidTaskExecutorException.class);
        }
    }
}
