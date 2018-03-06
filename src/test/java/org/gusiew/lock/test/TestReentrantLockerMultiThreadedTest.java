package org.gusiew.lock.test;

import org.gusiew.lock.impl.TestReentrantLocker;
import org.gusiew.lock.impl.TestReentrantMutex;
import org.gusiew.lock.impl.exception.MutexException;
import org.gusiew.lock.impl.exception.MutexHeldByOtherThreadException;
import org.gusiew.lock.test.util.ThreadSuspender;
import org.junit.jupiter.api.Test;

import java.util.*;
import java.util.function.BiConsumer;

import static java.lang.Thread.sleep;
import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

class TestReentrantLockerMultiThreadedTest extends AbstractReentrantLockerTest {

    private static final String THREAD_ONE = "Thread-1";
    private static final String THREAD_TWO = "Thread-2";
    private static final String THREAD_THREE = "Thread-3";
    private static final int DEFAULT_DELAY_IN_MILLIS = 100;
    private static final int ONE_SECOND = 1000;

    @Test
    void noLockingForNonCompetingThreads() throws InterruptedException {
        //when
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, basicScenario(VALUE_A));
        ScenarioThread secondThread = startAndDelay(THREAD_TWO, basicScenario(VALUE_B));
        ScenarioThread thirdThread = startAndDelay(THREAD_THREE, basicScenario(VALUE_C));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadSuspended(thirdThread);

        assertThreadHoldsActiveMutex(firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(secondThread, VALUE_B);
        assertThreadHoldsActiveMutex(thirdThread, VALUE_C);

        //then
        resumeAndDelay(firstThread, secondThread, thirdThread);

        //assert
        assertThreadCompleted(firstThread);
        assertThreadCompleted(secondThread);
        assertThreadCompleted(thirdThread);

        assertMutexNotActive(VALUE_A);
        assertMutexNotActive(VALUE_B);
        assertMutexNotActive(VALUE_C);
    }

    @Test
    void shouldSupportSecondThreadStartingAndFinishingEarlier() throws InterruptedException {
        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, basicScenario(VALUE_A));

        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);

        ScenarioThread secondThread = startAndDelay(THREAD_TWO, basicScenario(VALUE_B));

        assertThreadSuspended(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(secondThread, VALUE_B);

        //then
        resumeAndDelay(secondThread);

        //assert
        assertThreadSuspended(firstThread);
        assertThreadCompleted(secondThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);
        assertMutexNotActive(VALUE_B);


        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadCompleted(firstThread);
        assertMutexNotActive(VALUE_A);
    }

    @Test
    void shouldWaitForLockReleaseByOtherThreadEvenWhenInterrupted() throws InterruptedException {

        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, basicScenario(VALUE_A));
        ScenarioThread secondThread = startAndDelay(THREAD_TWO, basicScenario(VALUE_A));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);
        assertThreadHanging(secondThread);
        //assertNull(secondThread.getMutex());

        //then
        doInterruptAndDelay(secondThread);

        //assert
        assertThreadHanging(secondThread);
        //assertNull(secondThread.getMutex());

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadCompleted(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(secondThread, VALUE_A);

        //then resume second thread
        resumeAndDelay(secondThread);

        //assert
        assertThreadCompleted(secondThread);
        assertMutexNotActive(VALUE_A);
    }

    @Test
    void shouldAllowMultipleThreadsContendingForSameLock() throws InterruptedException {

        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, basicScenario(VALUE_A));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);

        //then
        ScenarioThread secondThread = startAndDelay(THREAD_TWO, basicScenario(VALUE_A));
        ScenarioThread thirdThread = startAndDelay(THREAD_THREE, basicScenario(VALUE_A));

        //then
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);
        assertThreadHanging(secondThread);
        assertThreadHanging(thirdThread);
        //assertNull(secondThread.getMutex());

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadCompleted(firstThread);
        ScenarioThread holdingThread = assertHoldsActiveMutex(Arrays.asList(secondThread, thirdThread), VALUE_A);
        ScenarioThread hangingThread = holdingThread.equals(secondThread) ? thirdThread : secondThread;

        //assert
        assertNotNull(holdingThread);
        assertThreadSuspended(holdingThread);
        assertThreadHanging(thirdThread);
        assertThreadHoldsActiveMutex(holdingThread, VALUE_A);

        //then
        resumeAndDelay(holdingThread);

        //assert
        assertThreadCompleted(holdingThread);
        assertThreadSuspended(hangingThread);
        assertThreadHoldsActiveMutex(hangingThread, VALUE_A);

        //then
        resumeAndDelay(hangingThread);

        //assert
        assertThreadCompleted(hangingThread);
        assertMutexNotActive(VALUE_A);
    }

    @Test
    void shouldAllowWaitingForValueLockWhileHoldingOtherAndObtainWhenAvailable() throws InterruptedException {

        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, twoLocksScenario(VALUE_A, VALUE_B));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);

        ScenarioThread secondThread = startAndDelay(THREAD_TWO, basicScenario(VALUE_B));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(secondThread, VALUE_B);

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadHanging(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(secondThread, VALUE_B);

        //then
        resumeAndDelay(secondThread);

        //assert
        assertThreadSuspended(firstThread);
        assertThreadCompleted(secondThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(firstThread, VALUE_B);

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_B);
        assertMutexNotActive(VALUE_A);

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadCompleted(firstThread);
        assertMutexNotActive(VALUE_A);
        assertMutexNotActive(VALUE_B);
    }

    @Test
    void shouldReleaseLockOnlyWhenReleasesEqualAcquires() throws InterruptedException {

        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, twoLocksScenario(VALUE_A, VALUE_A));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);

        ScenarioThread secondThread = startAndDelay(THREAD_TWO, basicScenario(VALUE_A));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHanging(secondThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);

        //then
        resumeAndDelay(firstThread);

        //assert acquire second time
        assertThreadSuspended(firstThread);
        assertThreadHanging(secondThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);

        //then
        resumeAndDelay(firstThread);

        //assert first release
        assertThreadSuspended(firstThread);
        assertThreadHanging(secondThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);

        //then second release
        resumeAndDelay(firstThread);

        //assert
        assertThreadCompleted(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(secondThread, VALUE_A);

        //then
        resumeAndDelay(secondThread);

        //assert
        assertThreadCompleted(secondThread);
        assertMutexNotActive(VALUE_A);
    }

    @Test
    void shouldThrowWhenThreadTriesToReleaseMutexOwnedByOtherThread() throws InterruptedException {

        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, basicScenario(VALUE_A));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);

        //then
        TestReentrantMutex m = firstThread.getMutex(VALUE_A);

        ScenarioThread secondThread = startAndDelay(THREAD_TWO, releaseOnlyScenario(m));
        assertThreadSuspended(firstThread);
        assertTrue(secondThread.completedWithException());
        assertTrue(secondThread.getException() instanceof MutexHeldByOtherThreadException);

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadCompleted(firstThread);
        assertMutexNotActive(VALUE_A);
    }

    @Test
    void threadReleasesLockWhenOtherAcquiring() throws InterruptedException {
        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, basicScenario(VALUE_A));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);

        //then
        TestReentrantLocker lockSuspendLocker = new TestReentrantLocker.Builder().withSuspender(createSuspender())
                .withSuspendDuringLocking()
                .build();

        ScenarioThread secondThread = startAndDelay(THREAD_ONE, basicScenario(VALUE_A), lockSuspendLocker);

        //assert
        assertThreadSuspended(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);

        //then
        resumeAndDelay(firstThread);
        //assert
        assertThreadCompleted(firstThread);
        assertThreadSuspended(secondThread);
        assertMutexActiveButNotHeld(VALUE_A);
        //then
        resumeAndDelay(secondThread);
        //assert
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(secondThread, VALUE_A);
        //then
        resumeAndDelay(secondThread);

        //assert
        assertThreadCompleted(firstThread);
        assertMutexNotActive(VALUE_A);

    }

    @Test
    void threadReleasesAndReacquiresLockWhenOtherAcquiring() throws InterruptedException {

        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, reacquireScenario(VALUE_A));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);

        //then
        TestReentrantLocker lockSuspendLocker = new TestReentrantLocker.Builder().withSuspender(createSuspender())
                .withSuspendDuringLocking()
                .build();

        ScenarioThread secondThread = startAndDelay(THREAD_ONE, basicScenario(VALUE_A), lockSuspendLocker);

        //assert
        assertThreadSuspended(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadSuspended(firstThread);
        assertThreadSuspended(secondThread);
        assertMutexActiveButNotHeld(VALUE_A);

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadSuspended(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadCompleted(firstThread);
        assertThreadSuspended(secondThread);
        assertMutexActiveButNotHeld(VALUE_A);

        //then
        resumeAndDelay(secondThread);

        //assert
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(secondThread, VALUE_A);

        //then
        resumeAndDelay(secondThread);

        //assert
        assertThreadCompleted(secondThread);
        assertMutexNotActive(VALUE_A);
    }

    @Test
    void simulateDeadlock() throws InterruptedException {

        TestReentrantLocker interruptionLocker = new TestReentrantLocker.Builder().withSuspender(createSuspender())
                .withThrowWhenInterrupted()
                .build();

        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, interruptionScenario(VALUE_A, VALUE_B));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);

        //then
        ScenarioThread secondThread = startAndDelay(THREAD_TWO, twoLocksScenario(VALUE_B, VALUE_A), interruptionLocker);

        //assert
        assertThreadSuspended(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(secondThread, VALUE_B);

        //then
        resumeAndDelay(firstThread, secondThread);

        assertThreadHanging(firstThread);
        assertThreadHanging(secondThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(secondThread, VALUE_B);

        //then
        delay(ONE_SECOND);

        //assert
        assertThreadHanging(firstThread);
        assertThreadHanging(secondThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(secondThread, VALUE_B);

        //then
        doInterruptAndDelay(firstThread);

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHanging(secondThread);
        assertThreadHoldsActiveMutex(firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(secondThread, VALUE_B);

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadCompleted(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(secondThread, VALUE_A);
        assertThreadHoldsActiveMutex(secondThread, VALUE_B);

        //then
        resumeAndDelay(secondThread);

        //assert
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(secondThread, VALUE_A);
        assertMutexNotActive(VALUE_B);

        //then
        resumeAndDelay(secondThread);
        assertThreadCompleted(secondThread);
        assertMutexNotActive(VALUE_A);
        assertMutexNotActive(VALUE_B);
    }

    private ScenarioThread assertHoldsActiveMutex(List<ScenarioThread> scenarioThreads, String value) {
        //TODO Nasty code needs improvement
        TestReentrantMutex activeMutex = TestReentrantMutex.getFromActiveMutexes(value);
        List<ScenarioThread> activeThreads = scenarioThreads.stream().filter(t -> t.equals(activeMutex.getHolderThread())).collect(toList());
        assertFalse(activeThreads.size() == 0);
        assertFalse(activeThreads.size() > 1);
        return activeThreads.get(0);
    }

    private void doInterruptAndDelay(ScenarioThread thread) throws InterruptedException {
        thread.interrupt();
        sleep(DEFAULT_DELAY_IN_MILLIS);
    }

    private void assertThreadHanging(ScenarioThread thread) {
        assertFalse(thread.isSuspended());
        assertTrue(thread.isAlive());
    }

    private void assertMutexNotActive(Object value) {
        assertNull(TestReentrantMutex.getFromActiveMutexes(value));
    }

    private void assertMutexActiveButNotHeld(Object value) {
        TestReentrantMutex activeMutex = TestReentrantMutex.getFromActiveMutexes(value);
        assertNotNull(activeMutex);
        assertNull(activeMutex.getHolderThread());
    }

    private void assertThreadHoldsActiveMutex(ScenarioThread thread, Object value) {
        TestReentrantMutex threadMutex = thread.getMutex(value);
        TestReentrantMutex activeMutex = TestReentrantMutex.getFromActiveMutexes(value);

        assertSame(threadMutex, activeMutex);
        assertNotNull(threadMutex);
        assertEquals(thread, threadMutex.getHolderThread());
    }

    private void assertThreadCompleted(ScenarioThread thread) {
        assertTrue(thread.completedSuccesfully());
        assertFalse(thread.isAlive());
    }

    private void assertThreadSuspended(ScenarioThread thread) {
        assertTrue(thread.isSuspended());
    }

    private BiConsumer<TestReentrantLocker, TestThreadMutexState> basicScenario(Object value) {
        return (l, ms) -> {
            TestReentrantMutex m = l.lock(value);
            ms.put(value, m);
            l.suspend();
            m.release();
        };
    }

    private BiConsumer<TestReentrantLocker, TestThreadMutexState> twoLocksScenario(Object value1, Object value2) {
        return (l, ms) -> {
            TestReentrantMutex m1 = l.lock(value1);
            ms.put(value1, m1);
            l.suspend();
            TestReentrantMutex m2 = l.lock(value2);
            ms.put(value2, m2);
            l.suspend();
            m1.release();
            l.suspend();
            m2.release();
        };
    }

    private BiConsumer<TestReentrantLocker, TestThreadMutexState> interruptionScenario(Object value1, Object value2) {
        return (l, ms) -> {
            TestReentrantMutex m = l.lock(value1);
            ms.put(value1, m);
            l.suspend();
            try {
                l.lock(value2);
            } catch (MutexException e) {
                l.suspend();
            }
            m.release();
        };
    }

    private BiConsumer<TestReentrantLocker, TestThreadMutexState> reacquireScenario(Object value) {
        return (l, ms) -> {
            TestReentrantMutex m1 = l.lock(value);
            ms.put(value, m1);
            l.suspend();
            m1.release();
            l.suspend();
            m1 = l.lock(value);
            ms.put(value, m1);
            l.suspend();
            m1.release();
        };
    }

    private BiConsumer<TestReentrantLocker, TestThreadMutexState> releaseOnlyScenario(TestReentrantMutex m) {
        return (l, ms) -> {
            m.release();
        };
    }

    private void resumeAndDelay(ScenarioThread... threads) throws InterruptedException {
        Arrays.stream(threads).forEach(ScenarioThread::resumeFromSuspension);
        sleep(DEFAULT_DELAY_IN_MILLIS);
    }

    private void delay(long millis) throws InterruptedException {
        sleep(millis);
    }

    private ThreadSuspender createSuspender() {
        return new ThreadSuspender();
    }

    private ScenarioThread startAndDelay(String threadName, BiConsumer<TestReentrantLocker, TestThreadMutexState> scenario)
            throws InterruptedException {

        TestReentrantLocker lockSuspendLocker = new TestReentrantLocker.Builder().build();
        return startAndDelay(threadName, scenario, lockSuspendLocker);
    }

    private ScenarioThread startAndDelay(String threadName, BiConsumer<TestReentrantLocker, TestThreadMutexState> scenario, TestReentrantLocker testLocker)
            throws InterruptedException {
        ScenarioThread thread = new ScenarioThread(threadName, testLocker, scenario);
        thread.start();
        sleep(DEFAULT_DELAY_IN_MILLIS);
        return thread;
    }

    private class ScenarioThread extends Thread {

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

        boolean completedSuccesfully() {
            return exception == null;
        }

        boolean completedWithException() {
            return exception != null;
        }

        Exception getException() {
            return exception;
        }

        TestReentrantMutex getMutex(Object value) {
            return threadState.get(value);
        }


    }

    private class TestThreadMutexState {
        private final Map<Object, TestReentrantMutex> map = Collections.synchronizedMap(new HashMap<>());

        synchronized TestReentrantMutex get(Object value) {
            return map.get(value);
        }

        synchronized void put(Object value, TestReentrantMutex testReentrantMutex) {
            map.put(value, testReentrantMutex);
        }
    }
}
