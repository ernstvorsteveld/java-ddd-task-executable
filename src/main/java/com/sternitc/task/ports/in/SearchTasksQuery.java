package com.sternitc.task.ports.in;

import com.sternitc.task.domain.model.InvalidPageRequestException;
import com.sternitc.task.domain.model.TaskSearchCriteria;
import java.util.Objects;

public record SearchTasksQuery(TaskSearchCriteria criteria, int pageNumber, int pageSize) {

    public static final int DEFAULT_PAGE_NUMBER = 0;
    public static final int DEFAULT_PAGE_SIZE = 20;

    public SearchTasksQuery {
        Objects.requireNonNull(criteria, "criteria must not be null");
        if (pageNumber < 0) {
            throw new InvalidPageRequestException("pageNumber must not be negative");
        }
        if (pageSize <= 0) {
            throw new InvalidPageRequestException("pageSize must be greater than 0");
        }
    }

    public static SearchTasksQuery of(TaskSearchCriteria criteria) {
        return new SearchTasksQuery(criteria, DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE);
    }
}
