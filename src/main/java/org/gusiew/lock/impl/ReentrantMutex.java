package org.gusiew.lock.impl;

import jdk.nashorn.internal.ir.annotations.Immutable;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.gusiew.lock.api.Mutex;
import org.gusiew.lock.impl.exception.MutexActiveButDifferent;
import org.gusiew.lock.impl.exception.MutexHeldByOtherThreadException;
import org.gusiew.lock.impl.exception.MutexNotActiveException;
import org.gusiew.lock.impl.util.StripedMap;

import static org.gusiew.lock.impl.util.ConditionUtil.not;
import static org.gusiew.lock.impl.util.ThreadUtil.sameThreads;

/**
 * Currently the only implementation of {@link org.gusiew.lock.api.Mutex}.
 * <p>Produced by {@link ReentrantLocker#lock}
 * <p> See {@link ReentrantLocker} for more details
 */
@ThreadSafe
public class ReentrantMutex implements Mutex {
    @Immutable
    private final Object lock;

    /**
     * See {@link StripedMap} for concurrency guarantees
     */
    private final StripedMap<Object, ReentrantMutex> locks;

    @GuardedBy("this")
    private Thread holderThread;
    @GuardedBy("this")
    private int entranceCount;
    @GuardedBy("this")
    private int waitingThreadsCount;

    protected ReentrantMutex(final Object value, StripedMap<Object, ReentrantMutex> locks) {
        //TODO Assume value immutability for now
        this.lock = value;
        this.locks = locks;
        this.holderThread = getCurrentThread();
        this.entranceCount = 1;
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
        synchronized (locks.getStripe(lock)) {
            validateWith(locks.get(lock));
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

    protected Object getLock() {
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

    synchronized boolean synchronizeAndAcquireLock() {
        boolean wasInterrupted = false;
        while (not(lockAvailable())) {
            wasInterrupted |= waitForLockAvailable();
        }
        acquireState();
        return wasInterrupted;
    }

    private boolean lockAvailable() {
        return holderThread == null;
    }

    private boolean waitForLockAvailable() {
        try {
            wait();
        } catch (InterruptedException e) {
            return handleInterruption();
        }
        return false;
    }

    protected boolean handleInterruption() {
        return true;
    }

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
            if(waitingThreadsCount == 0) {
                locks.remove(lock);
            } else {
                canNotify = true;
            }
        } else if(entranceCount < 0) {
            throw new IllegalStateException("Entrance count should never be less than 0");
        }
        return canNotify;
    }

    private void notifyLockAvailable() {
        notify();
    }

    protected Thread getHolderThread() {
        return holderThread;
    }

    protected boolean noWaitingThreads() {
        return waitingThreadsCount == 0;
    }

    protected int getEntranceCount() {
        return entranceCount;
    }

    protected int getWaitingThreadsCount() {
        return waitingThreadsCount;
    }

    protected void decreaseWaitingThreadsCount() {
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
