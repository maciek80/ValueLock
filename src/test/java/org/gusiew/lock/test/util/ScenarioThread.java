package org.gusiew.lock.test.util;

import org.gusiew.lock.impl.TestReentrantMutex;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class ScenarioThread extends Thread {

    private final ThreadSuspender threadSuspender;
    private final TestThreadMutexState threadState;
    private final BiConsumer<ThreadSuspender, TestThreadMutexState> scenario;
    private final Options options;
    private volatile Exception exception;
    private final static ThreadLocal<ThreadLocalContext> threadContext = ThreadLocal.withInitial(() -> ThreadLocalContext.EMPTY);

    ScenarioThread(String threadName, BiConsumer<ThreadSuspender, TestThreadMutexState> scenario, ScenarioThread.Options options) {
        super(threadName);
        this.threadSuspender = new ThreadSuspender();
        this.threadState = new TestThreadMutexState();
        this.scenario = scenario;
        this.options = options;
    }

    @Override
    public void run() {
        threadContext.set(new ThreadLocalContext(threadSuspender, options));
        try {
            scenario.accept(threadSuspender, threadState);
        } catch (Exception e) {
            exception = e;
        } finally {
            threadContext.remove();
        }
    }

    public static ThreadLocalContext getThreadContext() {
        return threadContext.get();
    }

    boolean isSuspended() {
        return threadSuspender.isSuspended();
    }

    void resumeFromSuspension() {
        threadSuspender.resume();
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

    public static class Options {

        static final Options NO_OPTIONS = builder().withSuspendDuringLocking(false).withThrowWhenInterrupted(false).build();

        private final boolean suspendDuringLocking;
        private final boolean throwWhenInterrupted;

        Options(boolean suspendDuringLocking, boolean throwWhenInterrupted) {
            this.suspendDuringLocking = suspendDuringLocking;
            this.throwWhenInterrupted = throwWhenInterrupted;
        }

        public boolean isSuspendDuringLocking() {
            return suspendDuringLocking;
        }

        public boolean isThrowWhenInterrupted() {
            return throwWhenInterrupted;
        }

        public static OptionsBuilder builder() {
            return new OptionsBuilder();
        }

        public static class OptionsBuilder {
            private boolean suspendDuringLocking;
            private boolean throwWhenInterrupted;

            private OptionsBuilder() {
            }

            public OptionsBuilder withSuspendDuringLocking(boolean suspendDuringLocking) {
                this.suspendDuringLocking = suspendDuringLocking;
                return this;
            }

            public OptionsBuilder withThrowWhenInterrupted(boolean throwWhenInterrupted) {
                this.throwWhenInterrupted = throwWhenInterrupted;
                return this;
            }

            public Options build() {
                return new Options(suspendDuringLocking, throwWhenInterrupted);
            }
        }
    }

    public static class ThreadLocalContext {

        static final ThreadLocalContext EMPTY = new ThreadLocalContext(ThreadSuspender.NULL_OBJECT, Options.NO_OPTIONS);

        private final ThreadSuspender suspender;
        private final Options options;

        ThreadLocalContext(ThreadSuspender suspender, Options options) {
            this.suspender = suspender;
            this.options = options;
        }

        public ThreadSuspender getSuspender() {
            return suspender;
        }

        public Options getOptions() {
            return options;
        }
    }
}
