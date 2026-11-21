package com.sternitc.task.domain.model;

import java.time.Instant;

public record PlanningWindow(Instant startBefore, Instant finishBefore) {

    public PlanningWindow {
        if (startBefore == null || finishBefore == null) {
            throw new InvalidPlanningWindowException("startBefore and finishBefore must not be null");
        }
        if (startBefore.isAfter(finishBefore)) {
            throw new InvalidPlanningWindowException("startBefore must not be after finishBefore");
        }
    }
}
