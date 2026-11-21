package com.sternitc.task.ports.in;

import com.sternitc.task.domain.model.CommandExecutionStatus;
import com.sternitc.task.domain.model.CommandId;
import com.sternitc.task.domain.model.CommandReceipt;
import com.sternitc.task.domain.model.PlanningWindow;
import com.sternitc.task.domain.model.ProcessName;
import com.sternitc.task.domain.model.StoredCommand;
import com.sternitc.task.domain.model.Task;
import com.sternitc.task.domain.model.TaskExecutor;
import com.sternitc.task.domain.model.TaskId;
import com.sternitc.task.domain.model.TaskSearchCriteria;
import com.sternitc.task.domain.model.TaskStatus;
import com.sternitc.task.domain.model.TaskType;
import com.sternitc.task.domain.model.UserId;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CommandQuery Inbound Port")
class CommandQueryTest extends InboundPortTestSupport {

    @Nested
    @DisplayName("Command Processing — Requirements 1 & 2")
    class CommandQuerySpec {

        @Test
        @DisplayName("1.1 & 2.1 persists valid command, publishes event, handles async, and exposes completed StoredCommand")
        void storesCommandAndProcessesAsynchronously() {
            var command = new PlanTaskCommand(
                    TaskType.DO_AANVRAAG,
                    ProcessName.PS_IE,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice"))
            );

            CommandReceipt receipt = planTaskUseCase.planTask(command);

            assertThat(receipt.commandId()).isNotNull();
            assertThat(receipt.timestamp()).isEqualTo(FIXED_NOW);

            StoredCommand completed = awaitCommandStatus(receipt, CommandExecutionStatus.COMPLETED);
            assertThat(completed.id()).isEqualTo(receipt.commandId());
            assertThat(completed.timestamp()).isEqualTo(FIXED_NOW);
            assertThat(completed.failureReason()).isEmpty();

            List<Task> tasks = taskQuery.search(SearchTasksQuery.of(TaskSearchCriteria.empty()));
            assertThat(tasks).hasSize(1);
            assertThat(tasks.getFirst().type()).isEqualTo(TaskType.DO_AANVRAAG);
        }

        @Test
        @DisplayName("2.2 exposes FAILED StoredCommand with failureReason when async command execution fails")
        void returnsFailedStoredCommandWithReason() {
            CommandReceipt receipt = updateTaskUseCase.updateTask(new UpdateTaskCommand(
                    new TaskId(UUID.randomUUID()),
                    TaskStatus.IN_PROGRESS,
                    new PlanningWindow(START_BEFORE, FINISH_BEFORE),
                    new TaskExecutor.User(new UserId("alice"))
            ));

            StoredCommand failed = awaitCommandStatus(receipt, CommandExecutionStatus.FAILED);
            assertThat(failed.id()).isEqualTo(receipt.commandId());
            assertThat(failed.failureReason()).isPresent();
            assertThat(failed.failureReason().orElseThrow()).contains("Task not found");
        }

        @Test
        @DisplayName("returns empty when querying an unknown CommandId")
        void returnsEmptyForUnknownCommandId() {
            Optional<StoredCommand> found = commandQuery.findById(new CommandId(UUID.randomUUID()));

            assertThat(found).isEmpty();
        }
    }
}
