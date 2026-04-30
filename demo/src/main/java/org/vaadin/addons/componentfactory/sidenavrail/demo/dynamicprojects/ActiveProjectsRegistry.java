/*
 * Copyright 2026 Vaadin Ltd.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package org.vaadin.addons.componentfactory.sidenavrail.demo.dynamicprojects;

import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.shared.Registration;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Session-scoped registry of active projects driving the dynamic-projects demo.
 * One instance per Vaadin session, lazily created on first access via
 * {@link #current()} and kept on the session as an attribute. Listeners
 * registered through {@link #addChangeListener(Consumer)} fire on every
 * effective transition; calling {@link #activate(String)} on an already-active
 * project (or {@link #deactivate(String)} on an inactive one) is a no-op.
 */
public final class ActiveProjectsRegistry {

    private final Set<String> active = new LinkedHashSet<>();
    private final List<Consumer<ChangeEvent>> listeners = new CopyOnWriteArrayList<>();

    private ActiveProjectsRegistry() {}

    public static ActiveProjectsRegistry current() {
        VaadinSession session = VaadinSession.getCurrent();
        ActiveProjectsRegistry registry = session.getAttribute(ActiveProjectsRegistry.class);
        if (registry == null) {
            registry = new ActiveProjectsRegistry();
            session.setAttribute(ActiveProjectsRegistry.class, registry);
        }
        return registry;
    }

    public void activate(String projectId) {
        if (active.add(projectId)) {
            fire(new ChangeEvent(projectId, true));
        }
    }

    public void deactivate(String projectId) {
        if (active.remove(projectId)) {
            fire(new ChangeEvent(projectId, false));
        }
    }

    public boolean isActive(String projectId) {
        return active.contains(projectId);
    }

    /** Unmodifiable snapshot of the current active set in insertion order. */
    public Set<String> getActive() {
        return Collections.unmodifiableSet(new LinkedHashSet<>(active));
    }

    public Registration addChangeListener(Consumer<ChangeEvent> listener) {
        listeners.add(listener);
        return () -> listeners.remove(listener);
    }

    private void fire(ChangeEvent event) {
        for (Consumer<ChangeEvent> listener : listeners) {
            listener.accept(event);
        }
    }

    /** Fired for every effective activate/deactivate transition. */
    public record ChangeEvent(String projectId, boolean active) {}
}
