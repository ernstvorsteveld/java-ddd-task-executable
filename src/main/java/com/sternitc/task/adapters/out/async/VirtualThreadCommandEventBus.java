package com.sternitc.task.adapters.out.async;

import com.sternitc.task.domain.model.CommandStoredEvent;
import com.sternitc.task.ports.out.CommandEventPublisher;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public final class VirtualThreadCommandEventBus implements CommandEventPublisher, AutoCloseable {

    private final List<Consumer<CommandStoredEvent>> listeners = new CopyOnWriteArrayList<>();
    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    public void subscribe(Consumer<CommandStoredEvent> listener) {
        Objects.requireNonNull(listener, "listener must not be null");
        listeners.add(listener);
    }

    @Override
    public void publish(CommandStoredEvent event) {
        Objects.requireNonNull(event, "event must not be null");
        for (Consumer<CommandStoredEvent> listener : listeners) {
            executor.execute(() -> listener.accept(event));
        }
    }

    @Override
    public void close() {
        executor.close();
    }
}
