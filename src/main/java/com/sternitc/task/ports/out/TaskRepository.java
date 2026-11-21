package com.sternitc.task.ports.out;

import com.sternitc.task.domain.model.Task;
import com.sternitc.task.domain.model.TaskExecutor;
import com.sternitc.task.domain.model.TaskId;
import com.sternitc.task.domain.model.TaskSearchCriteria;
import java.util.List;
import java.util.Optional;

public interface TaskRepository {

    Task save(Task task);

    Optional<Task> findById(TaskId taskId);

    List<Task> findByExecutor(TaskExecutor executor, int pageNumber, int pageSize);

    List<Task> search(TaskSearchCriteria criteria, int pageNumber, int pageSize);
}
