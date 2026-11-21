package com.sternitc.task.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public interface ExecutableContext {

    List<Predicate<Task>> predicates();

    default LoadedDomainObjects domainObjects() {
        return LoadedDomainObjects.empty();
    }

    default boolean evaluate(Task task) {
        Objects.requireNonNull(task, "task must not be null");
        return predicates().stream().allMatch(predicate -> predicate.test(task));
    }
}
