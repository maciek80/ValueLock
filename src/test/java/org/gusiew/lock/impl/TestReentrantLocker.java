package org.gusiew.lock.impl;

import org.gusiew.lock.test.util.ScenarioThread;

public class TestReentrantLocker extends ReentrantLocker {

    public TestReentrantLocker() {
        super();
    }

    public TestReentrantLocker(int concurrencyLevel) {
        super(concurrencyLevel);
    }

    @Override
    public TestReentrantMutex lock(Object value) {
        return (TestReentrantMutex)super.lock(value);
    }

    @Override
    protected TestReentrantMutex createAndLock(Object value) {
        ReentrantMutex reentrantMutex = super.createAndLock(value);
        return TestReentrantMutex.from(value, reentrantMutex, locks);
        //new TestReentrantMutex(value, entranceCount);
    }

    @Override
    protected void activeMutexesUpdated() {
        ScenarioThread.ThreadLocalContext c = ScenarioThread.getThreadContext();
        if(c.getOptions().isSuspendDuringLocking()) {
            c.getSuspender().suspend();
        }
    }

    public int getConcurrencyLevel() {
        return locks.getNumberOfStripes();
    }

    //FIXME Probably not needed anymore
    //TODO check locks
    public TestReentrantMutex getFromActiveMutexes(Object lock) {
        return (TestReentrantMutex) locks.get(lock);
    }

    public boolean isActiveMutex(Object value) {
        return getFromActiveMutexes(value) != null;
    }

    public boolean activeMutexesEmpty() {
        return locks.isEmpty();
    }

}