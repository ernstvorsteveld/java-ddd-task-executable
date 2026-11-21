package com.sternitc.task.domain.model;

public final class InvalidTaskTypeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidTaskTypeException(String message) {
        super(message);
    }
}
