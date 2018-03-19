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
 *   <li>Does not react to interruptions but propagates the status </li>
 *   <li>Mutual exclusive locking, may deadlock if used inappropriately</li>
 *   <li>If number of distinct locks and threads is known in advance,
 *        concurrency level can be set to relax synchronization.
 *        Concurrency level specifies number of stripes in locker lock registry.
 *        Each stripe is locked independently
 *   </li>
 * </ul>
 * <p>Assumes that value is immutable
 * <p>Lock entrances count and number of waiting threads are stored as ints so int max value is the limit
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

    /**
     * Default constructor, sets concurrency level to 16
     */
    public ReentrantLocker() {
        this(DEFAULT_NUMBER_OF_STRIPES);
    }

    /**
     * Creates ReentrantLocker with specified concurrency level
     * @param concurrencyLevel concurrency level to set, see class comments
     */
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
