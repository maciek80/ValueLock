package org.gusiew.lock.test;

import org.gusiew.lock.impl.ReentrantMutex;
import org.gusiew.lock.impl.TestReentrantLocker;
import org.gusiew.lock.impl.TestReentrantMutex;
import org.gusiew.lock.impl.exception.MutexActiveButDifferent;
import org.gusiew.lock.impl.exception.MutexNotActiveException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.gusiew.lock.test.util.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class TestReentrantLockerSingleThreadedTest extends AbstractReentrantLockerTest {

    private static final Long VALUE_1 = 1L;
    private static final Long VALUE_1_OTHER_INSTANCE = new Long(1L);
    private static final Long VALUE_2 = 2L;
    private static final int ZERO_ENTRIES = 0;
    private static final boolean IGNORE_INTERRUPTIONS = false;

    @Test
    void shouldThrowWhenLockingOnNull() {
        assertThrows(NullPointerException.class, () -> locker.lock(null));
    }

    //TODO Consider validating active mutexes sizes

    @Test
    void shouldAcquireMutexForCurrentThread() {
        //when
        TestReentrantMutex mutex = locker.lock(VALUE_A);
        //then
        assertEquals(VALUE_A, mutex.getLock());
        assertActiveAndHeldByCurrentThread(mutex);
        //teardown
        mutex.release();
    }

    @Test
    void shouldReleaseAcquiredMutex() {
        //when
        TestReentrantMutex mutex = locker.lock(VALUE_A);
        mutex.release();
        //then
        assertNotActive(mutex);
    }

    @Test
    void shouldThrowOnReleaseWithoutLocking() {
        //given
        TestReentrantMutex mutex = new TestReentrantMutex(VALUE_A, ZERO_ENTRIES, IGNORE_INTERRUPTIONS);
        assertActiveMutexesEmpty();
        //then
        assertThrows(MutexNotActiveException.class, mutex::release);
        assertActiveMutexesEmpty();
    }

    @Test
    void shouldThrowOnTooManyReleases() {
        //when
        TestReentrantMutex mutex = locker.lock(VALUE_A);
        mutex.release();
        //then
        assertThrows(MutexNotActiveException.class, mutex::release);
    }

    @ParameterizedTest
    @MethodSource("lockerValuePairsProvider")
    void shouldReturnSameMutexOnlyForSameValues(Fixture fixture) {
        //given
        TestReentrantLocker testLocker = fixture.left.locker;
        TestReentrantLocker otherTestLocker = fixture.right.locker;
        Object testValue = fixture.left.value;
        Object otherTestValue = fixture.right.value;
        //when
        TestReentrantMutex reentrantMutex = testLocker.lock(testValue);
        TestReentrantMutex otherReentrantMutex = otherTestLocker.lock(otherTestValue);
        //then
        assertActiveAndHeldByCurrentThread(reentrantMutex);
        assertActiveAndHeldByCurrentThread(otherReentrantMutex);
        assertEquals(fixture.expectedResult, reentrantMutex == otherReentrantMutex);

        //teardown
        reentrantMutex.release();
        otherReentrantMutex.release();
    }

    private static Stream<Fixture> lockerValuePairsProvider() {
        TestReentrantLocker testLocker = new TestReentrantLocker();
        TestReentrantLocker otherTestLocker = new TestReentrantLocker();
        return Stream.of(fixture(lockerAndValue(testLocker, VALUE_A), lockerAndValue(testLocker, VALUE_A), true),
                         fixture(lockerAndValue(testLocker, VALUE_A), lockerAndValue(testLocker, VALUE_A_OTHER_INSTANCE), true),
                         fixture(lockerAndValue(testLocker, VALUE_A), lockerAndValue(otherTestLocker, VALUE_A), true),
                         fixture(lockerAndValue(testLocker, VALUE_A), lockerAndValue(otherTestLocker, VALUE_A_OTHER_INSTANCE), true),
                         fixture(lockerAndValue(testLocker, VALUE_1), lockerAndValue(otherTestLocker, VALUE_1), true),
                         fixture(lockerAndValue(testLocker, VALUE_1), lockerAndValue(otherTestLocker, VALUE_1_OTHER_INSTANCE), true),
                         fixture(lockerAndValue(testLocker, VALUE_A), lockerAndValue(testLocker, VALUE_B), false),
                         fixture(lockerAndValue(testLocker, VALUE_A), lockerAndValue(otherTestLocker, VALUE_B), false),
                         fixture(lockerAndValue(testLocker, VALUE_1), lockerAndValue(testLocker, VALUE_2), false),
                         fixture(lockerAndValue(testLocker, VALUE_1), lockerAndValue(otherTestLocker, VALUE_2), false),
                         fixture(lockerAndValue(testLocker, VALUE_A), lockerAndValue(otherTestLocker, VALUE_1), false)
        );
    }

    @Test
    void shouldAllowReentryForSameLockOnSameThread() {
        //given
        List<Object> sameValuesToLock = Arrays.asList(VALUE_A, VALUE_A, VALUE_A_OTHER_INSTANCE);
        List<TestReentrantMutex> mutexes = sameValuesToLock.stream()
                                                           .map(locker::lock)
                                                           .collect(toList());
        TestReentrantMutex mutex = mutexes.get(0);

        int entrances = sameValuesToLock.size();

        //then
        assertActiveAndHeldByCurrentThreadWithEntrances(mutex, entrances);

        //then
        mutex.release();
        assertActiveAndHeldByCurrentThreadWithEntrances(mutex, --entrances);

        //then
        mutex.release();
        assertActiveAndHeldByCurrentThreadWithEntrances(mutex, --entrances);

        //then
        mutex.release();
        assertNotActive(mutex);
    }

    @Test
    void shouldReleaseInOtherOrder() {
        //given
        TestReentrantMutex mutexA = locker.lock(VALUE_A);
        TestReentrantMutex mutexB = locker.lock(VALUE_B);
        mutexB.release();
        mutexA.release();
        //then
        assertNotActive(mutexA);
        assertNotActive(mutexB);
    }

    @Test
    void shouldThrowWhenTryingReleaseOnReleasedMutexAndOtherMutexForSameValueLockedInBetweenReleases() {
        //given
        ReentrantMutex mutexA = locker.lock(VALUE_A);
        mutexA.release();
        ReentrantMutex mutexB = locker.lock(VALUE_A);

        //then
        assertThrows(MutexActiveButDifferent.class, mutexA::release);

        //teardown
        mutexB.release();
    }

    private static Fixture fixture(LockerAndValue lockerAndValue, LockerAndValue otherLockerAndValue, boolean expectedResult) {
        return new Fixture(lockerAndValue, otherLockerAndValue, expectedResult);
    }


    private static LockerAndValue lockerAndValue(TestReentrantLocker locker, Object value) {
        return new LockerAndValue(locker, value);
    }


    private static class Fixture {
        final private LockerAndValue left;
        final private LockerAndValue right;
        final private boolean expectedResult;

        Fixture(LockerAndValue left, LockerAndValue right, boolean expectedResult) {
            this.left = left;
            this.right = right;
            this.expectedResult = expectedResult;
        }
    }

    private static class LockerAndValue {
        private final TestReentrantLocker locker;
        private final Object value;

        LockerAndValue(TestReentrantLocker locker, Object value) {
            this.locker = locker;
            this.value = value;
        }

        @Override
        public String toString() {
            return "LockerAndValue{" +
                    "locker=" + locker +
                    ", value=" + value +
                    '}';
        }
    }
}
