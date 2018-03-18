package org.gusiew.lock.impl;

import org.gusiew.lock.api.Locker;
import org.gusiew.lock.util.StripedMap;

/**
 * Currently the only implementation of {@link Locker}.
 *
 * <p>Has synchronized/wait like behavior:
 * <ul>
 *   <li>Allows holder thread to reenter but needs to be released same number of times as acquired</li>
 *   <li>Has synchronized/wait like semantics (does not provide fairness)</li>
 *   <li>Ignores interruptions</li>
 *   <li>Mutual exclusive locking, may deadlock if used inappropriately</li>
 * <ul>
 * <p>Assumes that value is immutable
 */
public class ReentrantLocker implements Locker {

    //TODO Refactor to something readable
    final static StripedMap<Object, ReentrantMutex> LOCKS = new StripedMap<>(16);
    private static final int ONE_ENTRANCE = 1;

    @Override
    public ReentrantMutex lock(final Object value) {
        ReentrantMutex reentrantMutex;
        boolean tryAcquireState = false;
        boolean wasInterrupted = false;

        validate(value);

        synchronized (LOCKS.getStripe(value)) {
            reentrantMutex = LOCKS.get(value);
            if (reentrantMutex == null) {
                reentrantMutex = createAndLock(value);
                LOCKS.put(reentrantMutex.getLock(), reentrantMutex);
            } else {
                tryAcquireState = reentrantMutex.synchronizeAndTryAcquireState();
            }
        }

        activeMutexesUpdated();

        if (tryAcquireState) {
            wasInterrupted = reentrantMutex.synchronizeAndAcquireLock();
        }
        setInterruptionOnThreadIfNeeded(wasInterrupted);

        return reentrantMutex;
    }

    private void validate(Object value) {
        if(value == null) {
            throw new NullPointerException();
        }
    }

    ReentrantMutex createAndLock(Object value) {
        return new ReentrantMutex(value, ONE_ENTRANCE);
    }

    void activeMutexesUpdated() {}

    private void setInterruptionOnThreadIfNeeded(boolean wasInterrupted) {
        if(wasInterrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
