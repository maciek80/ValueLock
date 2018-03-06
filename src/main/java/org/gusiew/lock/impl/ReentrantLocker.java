package org.gusiew.lock.impl;

import org.gusiew.lock.api.Locker;

import java.util.Map;

import static org.gusiew.lock.util.ConditionUtil.not;

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

    private static final int ONE_ENTRANCE = 1;
    private final Map<Object, ReentrantMutex> ACTIVE_MUTEXES = ReentrantMutex.getActiveMutexes();

    @Override
    public ReentrantMutex lock(final Object value) {
        ReentrantMutex reentrantMutex;
        boolean tryAcquireState = false;

        synchronized (ACTIVE_MUTEXES) {
            reentrantMutex = ACTIVE_MUTEXES.get(value);
            if (reentrantMutex == null) {
                reentrantMutex = createReentrantMutex(value, ONE_ENTRANCE);
                ACTIVE_MUTEXES.put(reentrantMutex.getLock(), reentrantMutex);
            } else {
                synchronized (reentrantMutex) {
                    tryAcquireState = reentrantMutex.tryAcquireState();
                }
            }
        }

        activeMutexesUpdated();

        if (tryAcquireState) {
            synchronized (reentrantMutex) {
                acquireLock(reentrantMutex);
            }
        }

        return reentrantMutex;
    }

    private void acquireLock(ReentrantMutex reentrantMutex) {
        while (not(reentrantMutex.lockAvailable())) {
            reentrantMutex.waitForLockAvailable();
        }
        reentrantMutex.acquireState();
    }

    protected void activeMutexesUpdated() {}

    protected ReentrantMutex createReentrantMutex(Object value, int entranceCount) {
        return new ReentrantMutex(value, entranceCount);
    }

}
