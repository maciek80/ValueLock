package org.gusiew.lock.test.util;

import org.gusiew.lock.impl.TestReentrantLocker;
import org.gusiew.lock.impl.TestReentrantMutex;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ScenarioThread extends Thread {

    private final TestReentrantLocker threadLocker;
    private final TestThreadMutexState threadState;
    private final BiConsumer<TestReentrantLocker, TestThreadMutexState> scenario;
    private volatile Exception exception;

    ScenarioThread(String threadName, TestReentrantLocker threadLocker, BiConsumer<TestReentrantLocker, TestThreadMutexState> scenario) {
        super(threadName);
        this.threadLocker = threadLocker;
        this.threadState = new TestThreadMutexState();
        this.scenario = scenario;
    }

    @Override
    public void run() {
        try {
            scenario.accept(threadLocker, threadState);
        } catch (Exception e) {
            exception = e;
        }
    }

    boolean isSuspended() {
        return threadLocker.isSuspended();
    }

    void resumeFromSuspension() {
        threadLocker.resume();
    }

    boolean completedSuccessfully() {
        return exception == null;
    }

    public boolean completedWithException() {
        return exception != null;
    }

    public Exception getException() {
        return exception;
    }

    public TestReentrantMutex getMutex(Object value) {
        return threadState.get(value);
    }

    public class TestThreadMutexState {
        private final Map<Object, TestReentrantMutex> map = Collections.synchronizedMap(new HashMap<>());

        synchronized TestReentrantMutex get(Object value) {
            return map.get(value);
        }

        public synchronized void put(Object value, TestReentrantMutex testReentrantMutex) {
            map.put(value, testReentrantMutex);
        }
    }

}
