package org.gusiew.lock.test.impl;

import org.gusiew.lock.impl.exception.MutexException;
import org.gusiew.lock.impl.exception.MutexHeldByOtherThreadException;
import org.gusiew.lock.test.impl.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.gusiew.lock.test.impl.util.ScenarioThread.Options.builder;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TestReentrantLockerMultiThreadedTest extends AbstractReentrantLockerTest {

    private static final String THREAD_ONE = "Thread-1";
    private static final String THREAD_TWO = "Thread-2";
    private static final String THREAD_THREE = "Thread-3";
    private static final int ONE_SECOND = 1000;
    private static final int MINIMAL_DELAY = 10;

    @Test
    void noLockingForNonCompetingThreads() throws InterruptedException {
        //when
        ScenarioThread firstThread = ScenarioThreadDriver.startAndDelay(THREAD_ONE, basicScenario(VALUE_A));
        ScenarioThread secondThread = ScenarioThreadDriver.startAndDelay(THREAD_TWO, basicScenario(VALUE_B));
        ScenarioThread thirdThread = ScenarioThreadDriver.startAndDelay(THREAD_THREE, basicScenario(VALUE_C));

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertThreadSuspended(thirdThread);

        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        Assertions.assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);
        Assertions.assertThreadHoldsActiveMutex(locker, thirdThread, VALUE_C);

        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread, secondThread, thirdThread);

        //assert
        Assertions.assertThreadCompleted(firstThread);
        Assertions.assertThreadCompleted(secondThread);
        Assertions.assertThreadCompleted(thirdThread);

        Assertions.assertMutexNotActive(locker, VALUE_A);
        Assertions.assertMutexNotActive(locker, VALUE_B);
        Assertions.assertMutexNotActive(locker, VALUE_C);
    }

    @Test
    void shouldSupportSecondThreadStartingAndFinishingEarlier() throws InterruptedException {
        //given
        ScenarioThread firstThread = ScenarioThreadDriver.startAndDelay(THREAD_ONE, basicScenario(VALUE_A));

        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        ScenarioThread secondThread = ScenarioThreadDriver.startAndDelay(THREAD_TWO, basicScenario(VALUE_B));

        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        Assertions.assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);

        //then
        ScenarioThreadDriver.resumeAndDelay(secondThread);

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadCompleted(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        Assertions.assertMutexNotActive(locker, VALUE_B);


        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread);

        //assert
        Assertions.assertThreadCompleted(firstThread);
        Assertions.assertMutexNotActive(locker, VALUE_A);
    }

    @ParameterizedTest
    @MethodSource("valueAInstancesAndValueToCheckProvider")
    void shouldWaitForLockReleaseByOtherThread(ValuesTriple p) {

        Object firstThreadValue = p.first;
        Object secondThreadValue = p.second;
        Object valueToCheck = p.third;

        //given
        ScenarioThread firstThread = ScenarioThreadDriver.startAndDelay(THREAD_ONE, basicScenario(firstThreadValue));
        ScenarioThread secondThread = ScenarioThreadDriver.startAndDelay(THREAD_TWO, basicScenario(secondThreadValue));

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadHanging(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, valueToCheck);
        Assertions.assertThreadDidNotObtainMutex(secondThread, valueToCheck);
        Assertions.assertWaitingThreads(locker, valueToCheck, numberOfThreads(1));

        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread);

        //assert
        Assertions.assertThreadCompleted(firstThread);
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, secondThread, valueToCheck);
        Assertions.assertNoWaitingThreads(locker, valueToCheck);

        //then resume second thread
        ScenarioThreadDriver.resumeAndDelay(secondThread);

        //assert
        Assertions.assertThreadCompleted(secondThread);
        Assertions.assertMutexNotActive(locker, valueToCheck);
    }

    private static Stream<ValuesTriple> valueAInstancesAndValueToCheckProvider() {
        return getAllPossibleTriples();
    }

    private static Stream<ValuesTriple> getAllPossibleTriples() {
        return Stream.of(ValuesTriple.triple(VALUE_A, VALUE_A, VALUE_A),
                ValuesTriple.triple(VALUE_A, VALUE_A, VALUE_A_OTHER_INSTANCE),
                ValuesTriple.triple(VALUE_A, VALUE_A_OTHER_INSTANCE, VALUE_A),
                ValuesTriple.triple(VALUE_A, VALUE_A_OTHER_INSTANCE, VALUE_A_OTHER_INSTANCE),
                ValuesTriple.triple(VALUE_A_OTHER_INSTANCE, VALUE_A, VALUE_A),
                ValuesTriple.triple(VALUE_A_OTHER_INSTANCE, VALUE_A, VALUE_A_OTHER_INSTANCE),
                ValuesTriple.triple(VALUE_A_OTHER_INSTANCE, VALUE_A_OTHER_INSTANCE, VALUE_A),
                ValuesTriple.triple(VALUE_A_OTHER_INSTANCE, VALUE_A_OTHER_INSTANCE, VALUE_A_OTHER_INSTANCE)
        );
    }


    @Test
    void shouldPropagateInterruption() {
        //given
        ScenarioThread firstThread = ScenarioThreadDriver.startAndDelay(THREAD_ONE, basicScenario(VALUE_A));
        ScenarioThread secondThread = ScenarioThreadDriver.startAndDelay(THREAD_TWO, acquireInterruptionScenario(VALUE_A));

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadHanging(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        Assertions.assertThreadDidNotObtainMutex(secondThread, VALUE_A);
        Assertions.assertWaitingThreads(locker, VALUE_A, numberOfThreads(1));

        //then
        ScenarioThreadDriver.doInterruptAndDelay(secondThread);

        //assert
        Assertions.assertThreadHanging(secondThread);
        Assertions.assertWaitingThreads(locker, VALUE_A, numberOfThreads(1));

        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread);

        //assert
        Assertions.assertThreadCompleted(firstThread);
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, secondThread, VALUE_A);
        Assertions.assertNoWaitingThreads(locker, VALUE_A);

        //then resume second thread
        ScenarioThreadDriver.resumeAndDelay(secondThread);

        //assert
        Assertions.assertThreadCompleted(secondThread);
        Assertions.assertMutexNotActive(locker, VALUE_A);
    }

    @ParameterizedTest
    @MethodSource("valueAInstancesTriplesProvider")
    void shouldAllowMultipleThreadsContendingForSameLock(ValuesTriple t) {

        //given
        ScenarioThread firstThread = ScenarioThreadDriver.startAndDelay(THREAD_ONE, basicScenario(t.first));

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        Assertions.assertNoWaitingThreads(locker, VALUE_A);

        //then
        ScenarioThread secondThread = ScenarioThreadDriver.startAndDelay(THREAD_TWO, basicScenario(t.second));
        ScenarioThread thirdThread = ScenarioThreadDriver.startAndDelay(THREAD_THREE, basicScenario(t.third));

        //then
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadHanging(secondThread);
        Assertions.assertThreadHanging(thirdThread);

        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        Assertions.assertThreadDidNotObtainMutex(secondThread, VALUE_A);
        Assertions.assertThreadDidNotObtainMutex(thirdThread, VALUE_A);
        Assertions.assertWaitingThreads(locker, VALUE_A, numberOfThreads(2));

        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread);

        //assert
        Assertions.assertThreadCompleted(firstThread);
        ScenarioThread holdingThread = Assertions.assertHoldsActiveMutex(locker, Arrays.asList(secondThread, thirdThread), VALUE_A);
        ScenarioThread hangingThread = holdingThread.equals(secondThread) ? thirdThread : secondThread;

        //assert
        assertNotNull(holdingThread);
        Assertions.assertThreadSuspended(holdingThread);
        Assertions.assertThreadHanging(hangingThread);
        Assertions.assertWaitingThreads(locker, VALUE_A, numberOfThreads(1));

        Assertions.assertThreadHoldsActiveMutex(locker, holdingThread, VALUE_A);
        Assertions.assertThreadDidNotObtainMutex(hangingThread, VALUE_A);

        //then
        ScenarioThreadDriver.resumeAndDelay(holdingThread);

        //assert
        Assertions.assertThreadCompleted(holdingThread);
        Assertions.assertThreadSuspended(hangingThread);
        Assertions.assertThreadHoldsActiveMutex(locker, hangingThread, VALUE_A);
        Assertions.assertNoWaitingThreads(locker, VALUE_A);

        //then
        ScenarioThreadDriver.resumeAndDelay(hangingThread);

        //assert
        Assertions.assertThreadCompleted(hangingThread);
        Assertions.assertMutexNotActive(locker, VALUE_A);
    }

    private static Stream<ValuesTriple> valueAInstancesTriplesProvider() {
        return getAllPossibleTriples();
    }

    @Test
    void shouldAllowWaitingForValueLockWhileHoldingOtherAndObtainWhenAvailable() throws InterruptedException {

        //given
        ScenarioThread firstThread = ScenarioThreadDriver.startAndDelay(THREAD_ONE, twoLocksScenario(VALUE_A, VALUE_B));

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        ScenarioThread secondThread = ScenarioThreadDriver.startAndDelay(THREAD_TWO, basicScenario(VALUE_B));

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        Assertions.assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);

        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread);

        //assert
        Assertions.assertThreadHanging(firstThread);
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        Assertions.assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);

        //then
        ScenarioThreadDriver.resumeAndDelay(secondThread);

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadCompleted(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_B);

        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread);

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_B);
        Assertions.assertMutexNotActive(locker, VALUE_A);

        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread);

        //assert
        Assertions.assertThreadCompleted(firstThread);
        Assertions.assertMutexNotActive(locker, VALUE_A);
        Assertions.assertMutexNotActive(locker, VALUE_B);
    }

    @Test
    void shouldReleaseLockOnlyWhenReleasesEqualAcquires() throws InterruptedException {

        //given
        ScenarioThread firstThread = ScenarioThreadDriver.startAndDelay(THREAD_ONE, twoLocksScenario(VALUE_A, VALUE_A));

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        ScenarioThread secondThread = ScenarioThreadDriver.startAndDelay(THREAD_TWO, basicScenario(VALUE_A));

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadHanging(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread);

        //assert acquire second time
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadHanging(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread);

        //assert first release
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadHanging(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then second release
        ScenarioThreadDriver.resumeAndDelay(firstThread);

        //assert
        Assertions.assertThreadCompleted(firstThread);
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, secondThread, VALUE_A);

        //then
        ScenarioThreadDriver.resumeAndDelay(secondThread);

        //assert
        Assertions.assertThreadCompleted(secondThread);
        Assertions.assertMutexNotActive(locker, VALUE_A);
    }

    @Test
    void shouldThrowWhenThreadTriesToReleaseMutexOwnedByOtherThread() throws InterruptedException {

        //given
        ScenarioThread firstThread = ScenarioThreadDriver.startAndDelay(THREAD_ONE, basicScenario(VALUE_A));

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        TestReentrantMutex m = firstThread.getMutex(VALUE_A);

        ScenarioThread secondThread = ScenarioThreadDriver.startAndDelay(THREAD_TWO, releaseOnlyScenario(m));
        Assertions.assertThreadSuspended(firstThread);
        assertTrue(secondThread.completedWithException());
        assertTrue(secondThread.getException() instanceof MutexHeldByOtherThreadException);

        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread);

        //assert
        Assertions.assertThreadCompleted(firstThread);
        Assertions.assertMutexNotActive(locker, VALUE_A);
    }

    @Test
    void threadReleasesLockWhenOtherAcquiring() throws InterruptedException {
        //given
        ScenarioThread firstThread = ScenarioThreadDriver.startAndDelay(THREAD_ONE, basicScenario(VALUE_A));

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        ScenarioThread secondThread = ScenarioThreadDriver.startAndDelay(THREAD_ONE, basicScenario(VALUE_A), builder().withSuspendDuringLocking(true).build());

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread);
        //assert
        Assertions.assertThreadCompleted(firstThread);
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertMutexActiveButNotHeld(locker, VALUE_A);
        //then
        ScenarioThreadDriver.resumeAndDelay(secondThread);
        //assert
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, secondThread, VALUE_A);
        //then
        ScenarioThreadDriver.resumeAndDelay(secondThread);

        //assert
        Assertions.assertThreadCompleted(firstThread);
        Assertions.assertMutexNotActive(locker, VALUE_A);

    }

    @Test
    void threadReleasesAndReacquiresLockWhenOtherAcquiring() throws InterruptedException {

        //given
        ScenarioThread firstThread = ScenarioThreadDriver.startAndDelay(THREAD_ONE, reacquireScenario(VALUE_A));

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        ScenarioThread secondThread = ScenarioThreadDriver.startAndDelay(THREAD_ONE, basicScenario(VALUE_A), builder().withSuspendDuringLocking(true).build());

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread);

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertMutexActiveButNotHeld(locker, VALUE_A);

        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread);

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread);

        //assert
        Assertions.assertThreadCompleted(firstThread);
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertMutexActiveButNotHeld(locker, VALUE_A);

        //then
        ScenarioThreadDriver.resumeAndDelay(secondThread);

        //assert
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, secondThread, VALUE_A);

        //then
        ScenarioThreadDriver.resumeAndDelay(secondThread);

        //assert
        Assertions.assertThreadCompleted(secondThread);
        Assertions.assertMutexNotActive(locker, VALUE_A);
    }

    @Test
    void simulateDeadlock() throws InterruptedException {

        //given
        ScenarioThread firstThread = ScenarioThreadDriver.startAndDelay(THREAD_ONE, deadlockInterruptionScenario(VALUE_A, VALUE_B), builder().withThrowWhenInterrupted(true).build());

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        ScenarioThread secondThread = ScenarioThreadDriver.startAndDelay(THREAD_TWO, twoLocksScenario(VALUE_B, VALUE_A));

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        Assertions.assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);

        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread, secondThread);

        Assertions.assertThreadHanging(firstThread);
        Assertions.assertThreadHanging(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        Assertions.assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);

        //then
        ScenarioThreadDriver.delay(ONE_SECOND);

        //assert
        Assertions.assertThreadHanging(firstThread);
        Assertions.assertThreadHanging(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        Assertions.assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);

        //then
        ScenarioThreadDriver.doInterruptAndDelay(firstThread);

        //assert
        Assertions.assertThreadSuspended(firstThread);
        Assertions.assertThreadHanging(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        Assertions.assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);

        //then
        ScenarioThreadDriver.resumeAndDelay(firstThread);

        //assert
        Assertions.assertThreadCompleted(firstThread);
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, secondThread, VALUE_A);
        Assertions.assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);

        //then
        ScenarioThreadDriver.resumeAndDelay(secondThread);

        //assert
        Assertions.assertThreadSuspended(secondThread);
        Assertions.assertThreadHoldsActiveMutex(locker, secondThread, VALUE_A);
        Assertions.assertMutexNotActive(locker, VALUE_B);

        //then
        ScenarioThreadDriver.resumeAndDelay(secondThread);
        Assertions.assertThreadCompleted(secondThread);
        Assertions.assertMutexNotActive(locker, VALUE_A);
        Assertions.assertMutexNotActive(locker, VALUE_B);
    }


    //TODO grouping consumers in single class and maybe extract ?

    private BiConsumer<ThreadSuspender, ScenarioThread.TestThreadMutexState> basicScenario(Object value) {
        return (s, ms) -> {
            TestReentrantMutex m = locker.lock(value);
            ms.put(value, m);
            s.suspend();
            m.release();
        };
    }

    private BiConsumer<ThreadSuspender, ScenarioThread.TestThreadMutexState> acquireInterruptionScenario(Object value) {
        return (s, ms) -> {
            TestReentrantMutex m = locker.lock(value);
            ms.put(value, m);
            try {
                Thread.sleep(MINIMAL_DELAY);
            } catch (InterruptedException e) {
                s.suspend();
            }
            m.release();
        };
    }

    private BiConsumer<ThreadSuspender, ScenarioThread.TestThreadMutexState> twoLocksScenario(Object value1, Object value2) {
        return (s, ms) -> {
            TestReentrantMutex m1 = locker.lock(value1);
            ms.put(value1, m1);
            s.suspend();
            TestReentrantMutex m2 = locker.lock(value2);
            ms.put(value2, m2);
            s.suspend();
            m1.release();
            s.suspend();
            m2.release();
        };
    }

    private BiConsumer<ThreadSuspender, ScenarioThread.TestThreadMutexState> deadlockInterruptionScenario(Object value1, Object value2) {
        return (s, ms) -> {
            TestReentrantMutex m = locker.lock(value1);
            ms.put(value1, m);
            s.suspend();
            try {
                locker.lock(value2);
            } catch (MutexException e) {
                s.suspend();
            }
            m.release();
        };
    }

    private BiConsumer<ThreadSuspender, ScenarioThread.TestThreadMutexState> reacquireScenario(Object value) {
        return (s, ms) -> {
            TestReentrantMutex m1 = locker.lock(value);
            ms.put(value, m1);
            s.suspend();
            m1.release();
            s.suspend();
            m1 = locker.lock(value);
            ms.put(value, m1);
            s.suspend();
            m1.release();
        };
    }

    private BiConsumer<ThreadSuspender, ScenarioThread.TestThreadMutexState> releaseOnlyScenario(TestReentrantMutex m) {
        return (s, ms) -> m.release();
    }

    private static class ValuesTriple {

        private final Object first;
        private final Object second;
        private final Object third;

        private ValuesTriple(Object first, Object second, Object third) {
            this.first = first;
            this.second = second;
            this.third = third;
        }

        static ValuesTriple triple(Object first, Object second, Object third) {
            return new ValuesTriple(first, second, third);
        }
    }

    private int numberOfThreads(int n) {
        return n;
    }
}
