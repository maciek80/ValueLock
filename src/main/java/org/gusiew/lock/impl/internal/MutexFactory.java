package org.gusiew.lock.impl.internal;

import org.gusiew.lock.impl.ReentrantMutex;

@FunctionalInterface
public interface MutexFactory {
    ReentrantMutex createAndLock(Object value);
}
