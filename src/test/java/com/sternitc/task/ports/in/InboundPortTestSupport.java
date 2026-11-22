package com.sternitc.task.ports.in;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import com.sternitc.task.adapters.out.UuidCommandIdGenerator;
import com.sternitc.task.adapters.out.UuidTaskIdGenerator;
import com.sternitc.task.adapters.out.async.VirtualThreadCommandEventBus;
import com.sternitc.task.adapters.out.mongo.MongoCommandRepository;
import com.sternitc.task.adapters.out.mongo.MongoTaskRepository;
import com.sternitc.task.application.CommandQueryService;
import com.sternitc.task.application.PlanTaskCommandListener;
import com.sternitc.task.application.PlanTaskService;
import com.sternitc.task.application.TaskQueryService;
import com.sternitc.task.application.UpdateTaskCommandListener;
import com.sternitc.task.application.UpdateTaskService;
import com.sternitc.task.domain.model.CommandExecutionStatus;
import com.sternitc.task.domain.model.CommandReceipt;
import com.sternitc.task.domain.model.StoredCommand;
import com.sternitc.task.domain.model.Task;
import com.sternitc.task.domain.model.TaskCommandPayload;
import com.sternitc.task.domain.model.TaskSearchCriteria;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.testcontainers.mongodb.MongoDBContainer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

abstract class InboundPortTestSupport {

    protected static final Instant START_BEFORE = Instant.parse("2026-11-22T09:00:00Z");
    protected static final Instant FINISH_BEFORE = Instant.parse("2026-11-22T17:00:00Z");
    protected static final Instant FIXED_NOW = Instant.parse("2026-11-21T15:00:00Z");

    private static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8.0");
    private static final MongoClient MONGO_CLIENT;

    static {
        MONGO.start();
        MONGO_CLIENT = MongoClients.create(MONGO.getConnectionString());
    }

    protected MongoDatabase database;
    protected VirtualThreadCommandEventBus<TaskCommandPayload.PlanTask> planTaskEventBus;
    protected VirtualThreadCommandEventBus<TaskCommandPayload.UpdateTask> updateTaskEventBus;
    protected PlanTaskUseCase planTaskUseCase;
    protected UpdateTaskUseCase updateTaskUseCase;
    protected TaskQuery taskQuery;
    protected CommandQuery commandQuery;

    @BeforeEach
    void setUp() {
        database = MONGO_CLIENT.getDatabase("task_test_" + UUID.randomUUID().toString().replace("-", ""));
        var commandRepository = new MongoCommandRepository(database);
        var taskRepository = new MongoTaskRepository(database);
        var commandIdGenerator = new UuidCommandIdGenerator();
        var clock = Clock.fixed(FIXED_NOW, ZoneOffset.UTC);

        planTaskEventBus = new VirtualThreadCommandEventBus<>();
        updateTaskEventBus = new VirtualThreadCommandEventBus<>();

        var planTaskListener = new PlanTaskCommandListener(commandRepository, taskRepository, new UuidTaskIdGenerator());
        var updateTaskListener = new UpdateTaskCommandListener(commandRepository, taskRepository);
        planTaskEventBus.subscribe(planTaskListener::handle);
        updateTaskEventBus.subscribe(updateTaskListener::handle);

        planTaskUseCase = new PlanTaskService(commandRepository, planTaskEventBus, commandIdGenerator, clock);
        updateTaskUseCase = new UpdateTaskService(commandRepository, updateTaskEventBus, commandIdGenerator, clock);
        taskQuery = new TaskQueryService(taskRepository);
        commandQuery = new CommandQueryService(commandRepository);
    }

    @AfterEach
    void tearDown() {
        if (planTaskEventBus != null) {
            planTaskEventBus.close();
        }
        if (updateTaskEventBus != null) {
            updateTaskEventBus.close();
        }
    }

    protected Task planAndAwaitTask(PlanTaskCommand command) {
        int existingCount = taskQuery.search(new SearchTasksQuery(TaskSearchCriteria.empty(), 0, 100)).size();
        CommandReceipt receipt = planTaskUseCase.planTask(command);
        awaitCommandStatus(receipt, CommandExecutionStatus.COMPLETED);
        List<Task> all = taskQuery.search(new SearchTasksQuery(TaskSearchCriteria.empty(), 0, 100));
        assertThat(all).hasSize(existingCount + 1);
        return all.getLast();
    }

    protected StoredCommand awaitCommandStatus(CommandReceipt receipt, CommandExecutionStatus expectedStatus) {
        await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            Optional<StoredCommand> stored = commandQuery.findById(receipt.commandId());
            assertThat(stored).isPresent();
            assertThat(stored.orElseThrow().status()).isEqualTo(expectedStatus);
        });
        return commandQuery.findById(receipt.commandId()).orElseThrow();
    }
}
