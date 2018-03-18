package org.gusiew.lock.impl;

import org.gusiew.lock.test.util.ScenarioThread;

public class TestReentrantLocker extends ReentrantLocker {

    @Override
    public TestReentrantMutex lock(Object value) {
        return (TestReentrantMutex)super.lock(value);
    }

    @Override
    protected TestReentrantMutex createAndLock(Object value) {
        ReentrantMutex reentrantMutex = super.createAndLock(value);
        return TestReentrantMutex.from(value, reentrantMutex);
        //new TestReentrantMutex(value, entranceCount);
    }

    @Override
    protected void activeMutexesUpdated() {
        ScenarioThread.ThreadLocalContext c = ScenarioThread.getThreadContext();
        if(c.getOptions().isSuspendDuringLocking()) {
            c.getSuspender().suspend();
        }
    }

}