package com.sternitc.task.domain.model;

public final class InvalidLinkedEntityException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidLinkedEntityException(String message) {
        super(message);
    }
}
