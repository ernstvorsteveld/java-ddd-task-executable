package com.sternitc.task.ports.out;

import com.sternitc.task.domain.model.TaskId;

public interface TaskIdGenerator {

    TaskId nextId();
}
