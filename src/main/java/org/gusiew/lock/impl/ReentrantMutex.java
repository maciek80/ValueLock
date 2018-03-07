package org.gusiew.lock.impl;

import org.gusiew.lock.api.Mutex;
import org.gusiew.lock.impl.exception.MutexActiveButDifferent;
import org.gusiew.lock.impl.exception.MutexHeldByOtherThreadException;
import org.gusiew.lock.impl.exception.MutexNotActiveException;

import java.util.HashMap;
import java.util.Map;

import static org.gusiew.lock.util.ConditionUtil.not;
import static org.gusiew.lock.util.ThreadUtil.sameThreads;

/**
 * Currently the only implementation of {@link org.gusiew.lock.api.Mutex}.
 * <p>Produced by {@link ReentrantLocker}.
 */
public class ReentrantMutex implements Mutex {

    final static private Map<Object, ReentrantMutex> ACTIVE_MUTEXES = new HashMap<>();

    private final Object lock;
    private volatile Thread holderThread;
    private volatile int entranceCount;
    private volatile int waitingThreadsCount;

    ReentrantMutex(final Object value, int entranceCount) {
        //TODO Assume value immutability for now
        this.lock = value;
        this.holderThread = getCurrentThread();
        this.entranceCount = entranceCount;
    }

    /**
     * Mutex must be released same time as acquired
     *
     * @throws MutexNotActiveException if mutex is not active (e.g. was already released)
     * @throws MutexHeldByOtherThreadException if thread tries to release mutex owned by other thread
     * @throws MutexActiveButDifferent If mutex active but not same instance as registered in active mutexes,
     *                                  can happen if release called on released mutex and other mutex with
     *                                  same value created in meantime and active
     */
    @Override
    public void release() {
        synchronized (ACTIVE_MUTEXES) {
            validateWith(ACTIVE_MUTEXES.get(lock));
            synchronizeAndRelease();
        }
    }

    private void validateWith(ReentrantMutex reentrantMutex) {
        if(reentrantMutex == null) {
            throw new MutexNotActiveException();
        } else if(this != reentrantMutex) {
            throw new MutexActiveButDifferent();
        }
    }

    private synchronized void synchronizeAndRelease() {
        synchronized (this) {
            if (sameThreads(Thread.currentThread(), holderThread)) {
                boolean released = tryReleasingState();
                if (released) {
                    notifyLockAvailable();
                }
            } else {
                throw new MutexHeldByOtherThreadException();
            }
        }
    }

    Object getLock() {
        return lock;
    }

    synchronized boolean synchronizeAndTryAcquireState() {
        boolean sameThreads = sameThreads(getCurrentThread(), getHolderThread());
        if(sameThreads) {
            entranceCount++;
        } else {
            waitingThreadsCount++;
        }
        return not(sameThreads);
    }

    synchronized void synchronizeAndAcquireLock() {
        while (not(lockAvailable())) {
            waitForLockAvailable();
        }
        acquireState();
    }

    private boolean lockAvailable() {
        return holderThread == null;
    }

    private void waitForLockAvailable() {
        try {
            wait();
        } catch (InterruptedException e) {
            handleInterruption();
        }
    }

    void handleInterruption() {}

    private void acquireState() {
        waitingThreadsCount--;
        entranceCount++;
        holderThread = Thread.currentThread();
    }

    private boolean tryReleasingState() {
        boolean canNotify = false;
        entranceCount--;
        if(entranceCount == 0) {
            holderThread = null;
            canNotify = true;
            if(waitingThreadsCount == 0) {
                ACTIVE_MUTEXES.remove(lock);
            }
        } else if(entranceCount < 0) {
            throw new IllegalStateException("Entrance count should never be less than 0");
        }
        return canNotify;
    }

    private void notifyLockAvailable() {
        notify();
    }

    Thread getHolderThread() {
        return holderThread;
    }

    static Map<Object, ReentrantMutex> getActiveMutexes() {
        return ACTIVE_MUTEXES;
    }

    boolean noWaitingThreads() {
        return waitingThreadsCount == 0;
    }

    int getEntranceCount() {
        return entranceCount;
    }

    int getWaitingThreadsCount() {
        return waitingThreadsCount;
    }

    void decreaseWaitingThreadsCount() {
        waitingThreadsCount--;
    }

    private Thread getCurrentThread() {
        return Thread.currentThread();
    }


    @Override
    public String toString() {
        return "ReentrantMutex{" +
                "lock=" + lock +
                ", holderThread=" + holderThread +
                ", entranceCount=" + entranceCount +
                ", waitingThreadsCount=" + waitingThreadsCount +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        ReentrantMutex reentrantMutex = (ReentrantMutex) o;

        return lock.equals(reentrantMutex.lock);
    }

    @Override
    public int hashCode() {
        return lock.hashCode();
    }

}
