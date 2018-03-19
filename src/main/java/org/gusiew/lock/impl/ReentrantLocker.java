package org.gusiew.lock.impl;

import net.jcip.annotations.ThreadSafe;
import org.gusiew.lock.api.Locker;
import org.gusiew.lock.impl.internal.ActiveMutexesUpdatedHandler;
import org.gusiew.lock.impl.internal.MutexFactory;
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
@ThreadSafe
public class ReentrantLocker implements Locker {

    private static final int DEFAULT_NUMBER_OF_STRIPES = 16;

    /**
     * See {@link StripedMap} for concurrency guarantees
     */
    private final StripedMap<Object, ReentrantMutex> locks;

    private MutexFactory mutexFactory = this::createAndLock;
    private ActiveMutexesUpdatedHandler activeMutexesUpdatedHandler = () -> {};

    public ReentrantLocker() {
        this(DEFAULT_NUMBER_OF_STRIPES);
    }

    public ReentrantLocker(int concurrencyLevel) {
        locks = new StripedMap<>(concurrencyLevel);
    }

    @Override
    public ReentrantMutex lock(final Object value) {
        ReentrantMutex reentrantMutex;
        boolean tryAcquireState = false;
        boolean wasInterrupted = false;

        validate(value);

        synchronized (locks.getStripe(value)) {
            reentrantMutex = locks.get(value);
            if (reentrantMutex == null) {
                reentrantMutex = mutexFactory.createAndLock(value);
                locks.put(reentrantMutex.getLock(), reentrantMutex);
            } else {
                tryAcquireState = reentrantMutex.synchronizeAndTryAcquireState();
            }
        }

        activeMutexesUpdatedHandler.activeMutexesUpdated();

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

    private ReentrantMutex createAndLock(Object value) {
        return new ReentrantMutex(value, locks);
    }

    private void setInterruptionOnThreadIfNeeded(boolean wasInterrupted) {
        if(wasInterrupted) {
            Thread.currentThread().interrupt();
        }
    }
}
