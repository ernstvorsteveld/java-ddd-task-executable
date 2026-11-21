package com.sternitc.task.ports.in;

import com.sternitc.task.domain.model.InvalidPageRequestException;
import com.sternitc.task.domain.model.InvalidTaskExecutorException;
import com.sternitc.task.domain.model.TaskExecutor;

public record FindTasksByExecutorQuery(TaskExecutor executor, int pageNumber, int pageSize) {

    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;

    public FindTasksByExecutorQuery {
        if (executor == null) {
            throw new InvalidTaskExecutorException("executor must not be null");
        }
        if (pageNumber < 0) {
            throw new InvalidPageRequestException("pageNumber must not be negative");
        }
        if (pageSize <= 0) {
            throw new InvalidPageRequestException("pageSize must be greater than 0");
        }
    }

    public static FindTasksByExecutorQuery of(TaskExecutor executor) {
        return new FindTasksByExecutorQuery(executor, DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE);
    }
}
