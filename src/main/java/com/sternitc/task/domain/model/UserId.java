package com.sternitc.task.domain.model;

public record UserId(String value) {

    public UserId {
        if (value == null || value.isBlank()) {
            throw new InvalidTaskExecutorException("userId must not be null or blank");
        }
    }
}
