package com.sternitc.task.domain.model;

public final class InvalidTaskVersionException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidTaskVersionException(String message) {
        super(message);
    }
}
