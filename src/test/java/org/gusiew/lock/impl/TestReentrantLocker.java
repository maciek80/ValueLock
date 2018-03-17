package org.gusiew.lock.impl;

import org.gusiew.lock.test.util.ThreadSuspender;

public class TestReentrantLocker extends ReentrantLocker {

    private final ThreadSuspender threadSuspender;
    private boolean suspendDuringLocking = false;
    private boolean throwWhenInterrupted = false;

    public TestReentrantLocker() {
        super();
        threadSuspender = ThreadSuspender.NULL_OBJECT;
    }

    private TestReentrantLocker(ThreadSuspender threadSuspender, boolean suspendDuringLocking, boolean throwWhenInterrupted) {
        super();
        this.threadSuspender = threadSuspender;
        this.suspendDuringLocking = suspendDuringLocking;
        this.throwWhenInterrupted = throwWhenInterrupted;
    }

    @Override
    public TestReentrantMutex lock(Object value) {
        return (TestReentrantMutex)super.lock(value);
    }

    @Override
    protected TestReentrantMutex createAndLock(Object value) {
        ReentrantMutex reentrantMutex = super.createAndLock(value);
        return TestReentrantMutex.from(value, reentrantMutex, throwWhenInterrupted);
        //new TestReentrantMutex(value, entranceCount, throwWhenInterrupted);
    }

    @Override
    protected void activeMutexesUpdated() {
        if(suspendDuringLocking) {
            threadSuspender.suspend();
        }
    }

    public void suspend() {
        threadSuspender.suspend();
    }

    public void resume() {
        threadSuspender.resume();
    }

    public boolean isSuspended() {
        return threadSuspender.isSuspended();
    }

    public static class Builder {

        private boolean suspendDuringLocking = false;
        private boolean throwWhenInterrupted = false;
        private ThreadSuspender suspender;

        public Builder withSuspendDuringLocking() {
            suspendDuringLocking = true;
            return this;
        }

        public Builder withSuspender(ThreadSuspender suspender) {
            this.suspender = suspender;
            return this;
        }

        public TestReentrantLocker build() {
            ThreadSuspender s = suspender == null ? new ThreadSuspender() : suspender;
            return new TestReentrantLocker(s, suspendDuringLocking, throwWhenInterrupted);
        }

        public Builder withThrowWhenInterrupted() {
            throwWhenInterrupted = true;
            return this;
        }
    }
}