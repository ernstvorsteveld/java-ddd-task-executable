package com.sternitc.task.domain.model;

public final class InvalidTaskStatusException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidTaskStatusException(String message) {
        super(message);
    }
}
