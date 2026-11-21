package com.sternitc.task.domain.model;

public record RoleName(String value) {

    public RoleName {
        if (value == null || value.isBlank()) {
            throw new InvalidTaskExecutorException("roleName must not be null or blank");
        }
    }
}
