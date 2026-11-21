package com.sternitc.task.domain.model;

public record TaskVersion(int value) {

    public TaskVersion {
        if (value < 1) {
            throw new InvalidTaskVersionException("version must be at least 1");
        }
    }

    public static TaskVersion initial() {
        return new TaskVersion(1);
    }

    public TaskVersion next() {
        return new TaskVersion(Math.addExact(value, 1));
    }
}
