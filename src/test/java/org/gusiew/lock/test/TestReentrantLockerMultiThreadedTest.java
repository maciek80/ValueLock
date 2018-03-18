package org.gusiew.lock.test;

import org.gusiew.lock.impl.TestReentrantMutex;
import org.gusiew.lock.impl.exception.MutexException;
import org.gusiew.lock.impl.exception.MutexHeldByOtherThreadException;
import org.gusiew.lock.test.util.ScenarioThread;
import org.gusiew.lock.test.util.ThreadSuspender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.stream.Stream;

import static org.gusiew.lock.test.util.Assertions.*;
import static org.gusiew.lock.test.util.ScenarioThread.Options.builder;
import static org.gusiew.lock.test.util.ScenarioThreadDriver.*;
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
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, basicScenario(VALUE_A));
        ScenarioThread secondThread = startAndDelay(THREAD_TWO, basicScenario(VALUE_B));
        ScenarioThread thirdThread = startAndDelay(THREAD_THREE, basicScenario(VALUE_C));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadSuspended(thirdThread);

        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);
        assertThreadHoldsActiveMutex(locker, thirdThread, VALUE_C);

        //then
        resumeAndDelay(firstThread, secondThread, thirdThread);

        //assert
        assertThreadCompleted(firstThread);
        assertThreadCompleted(secondThread);
        assertThreadCompleted(thirdThread);

        assertMutexNotActive(locker, VALUE_A);
        assertMutexNotActive(locker, VALUE_B);
        assertMutexNotActive(locker, VALUE_C);
    }

    @Test
    void shouldSupportSecondThreadStartingAndFinishingEarlier() throws InterruptedException {
        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, basicScenario(VALUE_A));

        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        ScenarioThread secondThread = startAndDelay(THREAD_TWO, basicScenario(VALUE_B));

        assertThreadSuspended(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);

        //then
        resumeAndDelay(secondThread);

        //assert
        assertThreadSuspended(firstThread);
        assertThreadCompleted(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        assertMutexNotActive(locker, VALUE_B);


        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadCompleted(firstThread);
        assertMutexNotActive(locker, VALUE_A);
    }

    @ParameterizedTest
    @MethodSource("valueAInstancesAndValueToCheckProvider")
    void shouldWaitForLockReleaseByOtherThread(ValuesTriple p) {

        Object firstThreadValue = p.first;
        Object secondThreadValue = p.second;
        Object valueToCheck = p.third;

        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, basicScenario(firstThreadValue));
        ScenarioThread secondThread = startAndDelay(THREAD_TWO, basicScenario(secondThreadValue));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHanging(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, valueToCheck);
        assertThreadDidNotObtainMutex(secondThread, valueToCheck);
        assertWaitingThreads(locker, valueToCheck, numberOfThreads(1));

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadCompleted(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(locker, secondThread, valueToCheck);
        assertNoWaitingThreads(locker, valueToCheck);

        //then resume second thread
        resumeAndDelay(secondThread);

        //assert
        assertThreadCompleted(secondThread);
        assertMutexNotActive(locker, valueToCheck);
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
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, basicScenario(VALUE_A));
        ScenarioThread secondThread = startAndDelay(THREAD_TWO, acquireInterruptionScenario(VALUE_A));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHanging(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        assertThreadDidNotObtainMutex(secondThread, VALUE_A);
        assertWaitingThreads(locker, VALUE_A, numberOfThreads(1));

        //then
        doInterruptAndDelay(secondThread);

        //assert
        assertThreadHanging(secondThread);
        assertWaitingThreads(locker, VALUE_A, numberOfThreads(1));

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadCompleted(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(locker, secondThread, VALUE_A);
        assertNoWaitingThreads(locker, VALUE_A);

        //then resume second thread
        resumeAndDelay(secondThread);

        //assert
        assertThreadCompleted(secondThread);
        assertMutexNotActive(locker, VALUE_A);
    }

    @ParameterizedTest
    @MethodSource("valueAInstancesTriplesProvider")
    void shouldAllowMultipleThreadsContendingForSameLock(ValuesTriple t) {

        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, basicScenario(t.first));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        assertNoWaitingThreads(locker, VALUE_A);

        //then
        ScenarioThread secondThread = startAndDelay(THREAD_TWO, basicScenario(t.second));
        ScenarioThread thirdThread = startAndDelay(THREAD_THREE, basicScenario(t.third));

        //then
        assertThreadSuspended(firstThread);
        assertThreadHanging(secondThread);
        assertThreadHanging(thirdThread);

        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        assertThreadDidNotObtainMutex(secondThread, VALUE_A);
        assertThreadDidNotObtainMutex(thirdThread, VALUE_A);
        assertWaitingThreads(locker, VALUE_A, numberOfThreads(2));

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadCompleted(firstThread);
        ScenarioThread holdingThread = assertHoldsActiveMutex(locker, Arrays.asList(secondThread, thirdThread), VALUE_A);
        ScenarioThread hangingThread = holdingThread.equals(secondThread) ? thirdThread : secondThread;

        //assert
        assertNotNull(holdingThread);
        assertThreadSuspended(holdingThread);
        assertThreadHanging(hangingThread);
        assertWaitingThreads(locker, VALUE_A, numberOfThreads(1));

        assertThreadHoldsActiveMutex(locker, holdingThread, VALUE_A);
        assertThreadDidNotObtainMutex(hangingThread, VALUE_A);

        //then
        resumeAndDelay(holdingThread);

        //assert
        assertThreadCompleted(holdingThread);
        assertThreadSuspended(hangingThread);
        assertThreadHoldsActiveMutex(locker, hangingThread, VALUE_A);
        assertNoWaitingThreads(locker, VALUE_A);

        //then
        resumeAndDelay(hangingThread);

        //assert
        assertThreadCompleted(hangingThread);
        assertMutexNotActive(locker, VALUE_A);
    }

    private static Stream<ValuesTriple> valueAInstancesTriplesProvider() {
        return getAllPossibleTriples();
    }

    @Test
    void shouldAllowWaitingForValueLockWhileHoldingOtherAndObtainWhenAvailable() throws InterruptedException {

        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, twoLocksScenario(VALUE_A, VALUE_B));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        ScenarioThread secondThread = startAndDelay(THREAD_TWO, basicScenario(VALUE_B));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadHanging(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);

        //then
        resumeAndDelay(secondThread);

        //assert
        assertThreadSuspended(firstThread);
        assertThreadCompleted(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_B);

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_B);
        assertMutexNotActive(locker, VALUE_A);

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadCompleted(firstThread);
        assertMutexNotActive(locker, VALUE_A);
        assertMutexNotActive(locker, VALUE_B);
    }

    @Test
    void shouldReleaseLockOnlyWhenReleasesEqualAcquires() throws InterruptedException {

        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, twoLocksScenario(VALUE_A, VALUE_A));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        ScenarioThread secondThread = startAndDelay(THREAD_TWO, basicScenario(VALUE_A));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHanging(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        resumeAndDelay(firstThread);

        //assert acquire second time
        assertThreadSuspended(firstThread);
        assertThreadHanging(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        resumeAndDelay(firstThread);

        //assert first release
        assertThreadSuspended(firstThread);
        assertThreadHanging(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then second release
        resumeAndDelay(firstThread);

        //assert
        assertThreadCompleted(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(locker, secondThread, VALUE_A);

        //then
        resumeAndDelay(secondThread);

        //assert
        assertThreadCompleted(secondThread);
        assertMutexNotActive(locker, VALUE_A);
    }

    @Test
    void shouldThrowWhenThreadTriesToReleaseMutexOwnedByOtherThread() throws InterruptedException {

        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, basicScenario(VALUE_A));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

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
        assertMutexNotActive(locker, VALUE_A);
    }

    @Test
    void threadReleasesLockWhenOtherAcquiring() throws InterruptedException {
        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, basicScenario(VALUE_A));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        ScenarioThread secondThread = startAndDelay(THREAD_ONE, basicScenario(VALUE_A), builder().withSuspendDuringLocking(true).build());

        //assert
        assertThreadSuspended(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        resumeAndDelay(firstThread);
        //assert
        assertThreadCompleted(firstThread);
        assertThreadSuspended(secondThread);
        assertMutexActiveButNotHeld(locker, VALUE_A);
        //then
        resumeAndDelay(secondThread);
        //assert
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(locker, secondThread, VALUE_A);
        //then
        resumeAndDelay(secondThread);

        //assert
        assertThreadCompleted(firstThread);
        assertMutexNotActive(locker, VALUE_A);

    }

    @Test
    void threadReleasesAndReacquiresLockWhenOtherAcquiring() throws InterruptedException {

        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, reacquireScenario(VALUE_A));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        ScenarioThread secondThread = startAndDelay(THREAD_ONE, basicScenario(VALUE_A), builder().withSuspendDuringLocking(true).build());

        //assert
        assertThreadSuspended(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadSuspended(firstThread);
        assertThreadSuspended(secondThread);
        assertMutexActiveButNotHeld(locker, VALUE_A);

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadSuspended(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadCompleted(firstThread);
        assertThreadSuspended(secondThread);
        assertMutexActiveButNotHeld(locker, VALUE_A);

        //then
        resumeAndDelay(secondThread);

        //assert
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(locker, secondThread, VALUE_A);

        //then
        resumeAndDelay(secondThread);

        //assert
        assertThreadCompleted(secondThread);
        assertMutexNotActive(locker, VALUE_A);
    }

    @Test
    void simulateDeadlock() throws InterruptedException {

        //given
        ScenarioThread firstThread = startAndDelay(THREAD_ONE, deadlockInterruptionScenario(VALUE_A, VALUE_B), builder().withThrowWhenInterrupted(true).build());

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);

        //then
        ScenarioThread secondThread = startAndDelay(THREAD_TWO, twoLocksScenario(VALUE_B, VALUE_A));

        //assert
        assertThreadSuspended(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);

        //then
        resumeAndDelay(firstThread, secondThread);

        assertThreadHanging(firstThread);
        assertThreadHanging(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);

        //then
        delay(ONE_SECOND);

        //assert
        assertThreadHanging(firstThread);
        assertThreadHanging(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);

        //then
        doInterruptAndDelay(firstThread);

        //assert
        assertThreadSuspended(firstThread);
        assertThreadHanging(secondThread);
        assertThreadHoldsActiveMutex(locker, firstThread, VALUE_A);
        assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);

        //then
        resumeAndDelay(firstThread);

        //assert
        assertThreadCompleted(firstThread);
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(locker, secondThread, VALUE_A);
        assertThreadHoldsActiveMutex(locker, secondThread, VALUE_B);

        //then
        resumeAndDelay(secondThread);

        //assert
        assertThreadSuspended(secondThread);
        assertThreadHoldsActiveMutex(locker, secondThread, VALUE_A);
        assertMutexNotActive(locker, VALUE_B);

        //then
        resumeAndDelay(secondThread);
        assertThreadCompleted(secondThread);
        assertMutexNotActive(locker, VALUE_A);
        assertMutexNotActive(locker, VALUE_B);
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
