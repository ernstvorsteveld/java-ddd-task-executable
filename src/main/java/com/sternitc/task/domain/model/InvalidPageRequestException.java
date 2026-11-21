package com.sternitc.task.domain.model;

public final class InvalidPageRequestException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidPageRequestException(String message) {
        super(message);
    }
}
