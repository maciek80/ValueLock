package org.gusiew.lock.test.util;

import java.util.Arrays;
import java.util.function.BiConsumer;

import static java.lang.Thread.sleep;

public class ScenarioThreadDriver {

    //TODO Consider fluent interface and moving the assertions in

    private static final int DEFAULT_DELAY_IN_MILLIS = 100;

    public static ThreadSuspender createSuspender() {
        return new ThreadSuspender();
    }

    public static ScenarioThread startAndDelay(String threadName, BiConsumer<ThreadSuspender, ScenarioThread.TestThreadMutexState> scenario) {
        return startAndDelay(threadName, scenario, ScenarioThread.Options.NO_OPTIONS);
    }

    public static ScenarioThread startAndDelay(String threadName, BiConsumer<ThreadSuspender, ScenarioThread.TestThreadMutexState> scenario, ScenarioThread.Options options) {
        ScenarioThread thread = new ScenarioThread(threadName, scenario, options);
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
