package org.gusiew.lock.impl;

import org.gusiew.lock.impl.exception.MutexException;
import org.gusiew.lock.test.util.ScenarioThread;
import org.gusiew.lock.util.StripedMap;

import static org.gusiew.lock.util.ThreadUtil.sameThreads;

public class TestReentrantMutex extends ReentrantMutex {

    //private final boolean throwWhenInterrupted;

    public TestReentrantMutex(Object value, int entranceCount, StripedMap<Object, ReentrantMutex> locks) {
        super(value, entranceCount, locks);
        //this.throwWhenInterrupted = throwWhenInterrupted;
    }

    static TestReentrantMutex from(Object value, ReentrantMutex reentrantMutex, StripedMap<Object, ReentrantMutex> locks) {
        //FIXME - this is risky cause some of the reentrantMutex properties are ignored, use reflection ?
        return new TestReentrantMutex(value, reentrantMutex.getEntranceCount(), locks);
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
