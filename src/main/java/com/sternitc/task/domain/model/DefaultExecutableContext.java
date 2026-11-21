package com.sternitc.task.domain.model;

import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;

public class DefaultExecutableContext implements ExecutableContext {

    private final LoadedDomainObjects domainObjects;
    private final List<Predicate<Task>> predicates;

    public DefaultExecutableContext() {
        this(LoadedDomainObjects.empty(), List.of());
    }

    public DefaultExecutableContext(List<Predicate<Task>> predicates) {
        this(LoadedDomainObjects.empty(), predicates);
    }

    public DefaultExecutableContext(LoadedDomainObjects domainObjects, List<Predicate<Task>> predicates) {
        this.domainObjects = Objects.requireNonNull(domainObjects, "domainObjects must not be null");
        Objects.requireNonNull(predicates, "predicates must not be null");
        for (Predicate<Task> predicate : predicates) {
            Objects.requireNonNull(predicate, "predicate must not be null");
        }
        this.predicates = List.copyOf(predicates);
    }

    @Override
    public LoadedDomainObjects domainObjects() {
        return domainObjects;
    }

    @Override
    public List<Predicate<Task>> predicates() {
        return predicates;
    }
}
