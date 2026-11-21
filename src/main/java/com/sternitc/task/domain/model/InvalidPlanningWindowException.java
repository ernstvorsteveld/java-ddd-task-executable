package com.sternitc.task.domain.model;

public final class InvalidPlanningWindowException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public InvalidPlanningWindowException(String message) {
        super(message);
    }
}
