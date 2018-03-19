package org.gusiew.lock.impl.util;

import org.gusiew.lock.impl.ReentrantMutex;
import org.gusiew.lock.impl.exception.MutexException;
import org.gusiew.lock.test.util.ReflectionUtil;
import org.gusiew.lock.test.util.ScenarioThread;
import org.gusiew.lock.util.StripedMap;

import static org.gusiew.lock.util.ThreadUtil.sameThreads;

public class TestReentrantMutex extends ReentrantMutex {

    private static final String LOCK_FIELD_NAME = "lock";
    private static final String LOCKS_FIELD_NAME = "locks";

    private TestReentrantMutex(Object value, StripedMap<Object, ReentrantMutex> locks) {
        super(value, locks);
    }

    static TestReentrantMutex from(ReentrantMutex reentrantMutex) {
        Object lock = ReflectionUtil.getValue(reentrantMutex, LOCK_FIELD_NAME, Object.class);
        StripedMap<Object, ReentrantMutex> locks = ReflectionUtil.getValue(reentrantMutex, LOCKS_FIELD_NAME, StripedMap.class);
        return new TestReentrantMutex(lock, locks);
    }

    public boolean isHeld() {
        return getHolderThread() != null;
    }

    public boolean isHeldByCurrentThread() {
        return sameThreads(Thread.currentThread(), getHolderThread());
    }

    @Override
    public Thread getHolderThread() {
        return super.getHolderThread();
    }

    @Override
    public boolean noWaitingThreads() {
        return super.noWaitingThreads();
    }

    @Override
    public int getWaitingThreadsCount() {
        return super.getWaitingThreadsCount();
    }

    @Override
    public Object getLock() {
        return super.getLock();
    }

    @Override
    public int getEntranceCount() {
        return super.getEntranceCount();
    }

    public boolean noEntrances() {
        return getEntranceCount() == 0;
    }

    @Override
    protected boolean handleInterruption() {
        ScenarioThread.ThreadLocalContext c = ScenarioThread.getThreadContext();
        if(c.getOptions().isThrowWhenInterrupted()) {
            decreaseWaitingThreadsCount();
            throw new MutexException();
        }
        return super.handleInterruption();
    }
}
