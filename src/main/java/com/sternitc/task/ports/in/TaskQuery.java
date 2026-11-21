package com.sternitc.task.ports.in;

import com.sternitc.task.domain.model.Task;
import com.sternitc.task.domain.model.TaskId;
import java.util.List;
import java.util.Optional;

public interface TaskQuery {

    Optional<Task> findById(TaskId taskId);

    List<Task> findByExecutor(FindTasksByExecutorQuery query);

    List<Task> search(SearchTasksQuery query);
}
