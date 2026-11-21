package com.sternitc.task.domain.model;

public final class InvalidTaskExecutorException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidTaskExecutorException(String message) {
        super(message);
    }
}
