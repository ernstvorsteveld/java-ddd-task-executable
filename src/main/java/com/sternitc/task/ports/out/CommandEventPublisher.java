package com.sternitc.task.ports.out;

import com.sternitc.task.domain.model.CommandStoredEvent;

public interface CommandEventPublisher {

    void publish(CommandStoredEvent event);
}
