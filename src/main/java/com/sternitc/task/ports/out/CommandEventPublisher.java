package com.sternitc.task.ports.out;

import com.sternitc.task.domain.model.CommandPayload;
import com.sternitc.task.domain.model.CommandStoredEvent;

public interface CommandEventPublisher<T extends CommandPayload> {

    void publish(CommandStoredEvent<T> event);
}
