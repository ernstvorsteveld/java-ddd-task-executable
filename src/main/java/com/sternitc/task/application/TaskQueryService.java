package com.sternitc.task.application;

import com.sternitc.task.domain.model.Task;
import com.sternitc.task.domain.model.TaskId;
import com.sternitc.task.ports.in.FindTasksByExecutorQuery;
import com.sternitc.task.ports.in.SearchTasksQuery;
import com.sternitc.task.ports.in.TaskQuery;
import com.sternitc.task.ports.out.TaskRepository;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TaskQueryService implements TaskQuery {

    private final TaskRepository taskRepository;
    private final ExecutableContextResolver executableContextResolver;

    public TaskQueryService(TaskRepository taskRepository) {
        this(taskRepository, ExecutableContextResolver.defaults());
    }

    public TaskQueryService(TaskRepository taskRepository, ExecutableContextResolver executableContextResolver) {
        this.taskRepository = Objects.requireNonNull(taskRepository, "taskRepository must not be null");
        this.executableContextResolver = Objects.requireNonNull(
                executableContextResolver,
                "executableContextResolver must not be null"
        );
    }

    @Override
    public Optional<Task> findById(TaskId taskId) {
        Objects.requireNonNull(taskId, "taskId must not be null");
        return taskRepository.findById(taskId);
    }

    @Override
    public List<Task> findByExecutor(FindTasksByExecutorQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return filterExecutable(
                taskRepository.findByExecutor(query.executor(), query.pageNumber(), query.pageSize())
        );
    }

    @Override
    public List<Task> search(SearchTasksQuery query) {
        Objects.requireNonNull(query, "query must not be null");
        return filterExecutable(
                taskRepository.search(query.criteria(), query.pageNumber(), query.pageSize())
        );
    }

    private List<Task> filterExecutable(List<Task> tasks) {
        return tasks.stream()
                .filter(task -> task.isExecutable(executableContextResolver.resolve(task)))
                .toList();
    }
}
