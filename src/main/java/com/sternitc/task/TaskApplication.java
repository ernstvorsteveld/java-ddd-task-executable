package com.sternitc.task;

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
import com.sternitc.task.domain.model.TaskCommandPayload;
import com.sternitc.task.ports.in.CommandQuery;
import com.sternitc.task.ports.in.PlanTaskUseCase;
import com.sternitc.task.ports.in.TaskQuery;
import com.sternitc.task.ports.in.UpdateTaskUseCase;
import com.sternitc.task.ports.out.CommandIdGenerator;
import com.sternitc.task.ports.out.CommandRepository;
import com.sternitc.task.ports.out.TaskIdGenerator;
import com.sternitc.task.ports.out.TaskRepository;
import java.time.Clock;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class TaskApplication {

    public static void main(String[] args) {
        SpringApplication.run(TaskApplication.class, args);
    }

    @Bean
    @ConditionalOnMissingBean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean(destroyMethod = "close")
    MongoClient mongoClient(@Value("${task.mongodb.uri:mongodb://localhost:27017}") String uri) {
        return MongoClients.create(uri);
    }

    @Bean
    MongoDatabase mongoDatabase(
            MongoClient mongoClient,
            @Value("${task.mongodb.database:task}") String databaseName
    ) {
        return mongoClient.getDatabase(databaseName);
    }

    @Bean
    CommandRepository commandRepository(MongoDatabase mongoDatabase) {
        return new MongoCommandRepository(mongoDatabase);
    }

    @Bean
    TaskRepository taskRepository(MongoDatabase mongoDatabase) {
        return new MongoTaskRepository(mongoDatabase);
    }

    @Bean
    TaskIdGenerator taskIdGenerator() {
        return new UuidTaskIdGenerator();
    }

    @Bean
    CommandIdGenerator commandIdGenerator() {
        return new UuidCommandIdGenerator();
    }

    @Bean(destroyMethod = "close")
    VirtualThreadCommandEventBus<TaskCommandPayload.PlanTask> planTaskEventBus(
            CommandRepository commandRepository,
            TaskRepository taskRepository,
            TaskIdGenerator taskIdGenerator
    ) {
        var bus = new VirtualThreadCommandEventBus<TaskCommandPayload.PlanTask>();
        var listener = new PlanTaskCommandListener(commandRepository, taskRepository, taskIdGenerator);
        bus.subscribe(listener::handle);
        return bus;
    }

    @Bean(destroyMethod = "close")
    VirtualThreadCommandEventBus<TaskCommandPayload.UpdateTask> updateTaskEventBus(
            CommandRepository commandRepository,
            TaskRepository taskRepository
    ) {
        var bus = new VirtualThreadCommandEventBus<TaskCommandPayload.UpdateTask>();
        var listener = new UpdateTaskCommandListener(commandRepository, taskRepository);
        bus.subscribe(listener::handle);
        return bus;
    }

    @Bean
    PlanTaskUseCase planTaskUseCase(
            CommandRepository commandRepository,
            VirtualThreadCommandEventBus<TaskCommandPayload.PlanTask> planTaskEventBus,
            CommandIdGenerator commandIdGenerator,
            Clock clock
    ) {
        return new PlanTaskService(commandRepository, planTaskEventBus, commandIdGenerator, clock);
    }

    @Bean
    UpdateTaskUseCase updateTaskUseCase(
            CommandRepository commandRepository,
            VirtualThreadCommandEventBus<TaskCommandPayload.UpdateTask> updateTaskEventBus,
            CommandIdGenerator commandIdGenerator,
            Clock clock
    ) {
        return new UpdateTaskService(commandRepository, updateTaskEventBus, commandIdGenerator, clock);
    }

    @Bean
    TaskQuery taskQuery(TaskRepository taskRepository) {
        return new TaskQueryService(taskRepository);
    }

    @Bean
    CommandQuery commandQuery(CommandRepository commandRepository) {
        return new CommandQueryService(commandRepository);
    }
}
