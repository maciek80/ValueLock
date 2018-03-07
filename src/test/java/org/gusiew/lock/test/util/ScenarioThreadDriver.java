package org.gusiew.lock.test.util;

import org.gusiew.lock.impl.TestReentrantLocker;

import java.util.Arrays;
import java.util.function.BiConsumer;

import static java.lang.Thread.sleep;

public class ScenarioThreadDriver {

    //TODO Consider fluent interface and moving the assertions in

    private static final int DEFAULT_DELAY_IN_MILLIS = 100;

    public static ThreadSuspender createSuspender() {
        return new ThreadSuspender();
    }

    public static ScenarioThread startAndDelay(String threadName, BiConsumer<TestReentrantLocker, ScenarioThread.TestThreadMutexState> scenario) {
        TestReentrantLocker lockSuspendLocker = new TestReentrantLocker.Builder().build();
        return startAndDelay(threadName, scenario, lockSuspendLocker);
    }

    public static ScenarioThread startAndDelay(String threadName, BiConsumer<TestReentrantLocker, ScenarioThread.TestThreadMutexState> scenario, TestReentrantLocker testLocker) {
        ScenarioThread thread = new ScenarioThread(threadName, testLocker, scenario);
        thread.start();
        defaultDelay();
        return thread;
    }

    public static void resumeAndDelay(ScenarioThread... threads) {
        Arrays.stream(threads).forEach(ScenarioThread::resumeFromSuspension);
        defaultDelay();
    }

    public static void doInterruptAndDelay(ScenarioThread thread) {
        thread.interrupt();
        defaultDelay();
    }

    private static void defaultDelay() {
        delay(DEFAULT_DELAY_IN_MILLIS);
    }

    public static void delay(long millis) {
        try {
            sleep(millis);
        } catch (InterruptedException e) {
            throw new RuntimeException("Sleep interrupted");
        }
    }
}
