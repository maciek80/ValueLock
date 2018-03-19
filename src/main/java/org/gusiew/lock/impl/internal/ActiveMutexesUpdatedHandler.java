package org.gusiew.lock.impl.internal;

@FunctionalInterface
public interface ActiveMutexesUpdatedHandler {
    void activeMutexesUpdated();
}
