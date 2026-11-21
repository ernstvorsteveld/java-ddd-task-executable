package com.sternitc.task.domain.model;

public final class InvalidProcessNameException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidProcessNameException(String message) {
        super(message);
    }
}
