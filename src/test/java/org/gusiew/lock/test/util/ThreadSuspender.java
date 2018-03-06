package org.gusiew.lock.test.util;

import static org.gusiew.lock.util.ConditionUtil.not;

/**
 * Util class for thread coordination. Coordinated thread suspends and coordinator thread can resume it
 */
public class ThreadSuspender {

    private volatile boolean suspended = false;

    public static final ThreadSuspender NULL_OBJECT = new ThreadSuspender() {
        @Override
        public void suspend() {}

        @Override
        public void resume() {}

        @Override
        public boolean isSuspended() {
            return false;
        }
    };

    /**
     * @throws IllegalStateException if already suspended
     */
    public synchronized void suspend() {
        if(suspended) {
            throw new IllegalStateException("Thread already suspended");
        }
        suspended = true;
        while(suspended) {
            try {
                wait();
            } catch (Throwable e) {
                throw new RuntimeException("Thread " + Thread.currentThread().getName() + " suspension interrupted ", e);
            }
        }
    }

    /**
     * @throws IllegalStateException if not suspended
     */
    public synchronized void resume() {
        if(not(suspended)) {
            throw new IllegalStateException("Thread not suspended");
        }
        suspended = false;
        notify();
    }

    public boolean isSuspended() {
       return suspended;
    }

}
