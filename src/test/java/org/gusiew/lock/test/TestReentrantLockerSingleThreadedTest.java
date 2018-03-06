package org.gusiew.lock.test;

import org.gusiew.lock.impl.TestReentrantLocker;
import org.gusiew.lock.impl.TestReentrantMutex;
import org.gusiew.lock.impl.exception.MutexNotActiveException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

class TestReentrantLockerSingleThreadedTest extends AbstractReentrantLockerTest {

    private static final String VALUE_A_OTHER_INSTANCE = new String(VALUE_A);

    private static final Long VALUE_1 = 1L;
    private static final Long VALUE_1_OTHER_INSTANCE = new Long(1L);
    private static final Long VALUE_2 = 2L;
    private static final int ZERO_ENTRIES = 0;
    private static final boolean IGNORE_INTERRUPTIONS = false;

    @Test
    void shouldAcquireMutexForCurrentThread() {
        //when
        TestReentrantMutex mutex = locker.lock(VALUE_A);
        //then
        assertEquals(VALUE_A, mutex.getLock());
        assertTrue(mutex.isHeldByCurrentThread());
        assertTrue(mutex.noWaitingThreads());
        assertTrue(TestReentrantMutex.isActiveMutex(mutex.getLock()));
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
        assertNull(TestReentrantMutex.getFromActiveMutexes(VALUE_A));
        //then
        assertThrows(MutexNotActiveException.class, mutex::release);
        assertNull(TestReentrantMutex.getFromActiveMutexes(VALUE_A));
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
        assertNotNull(reentrantMutex);
        assertNotNull(otherReentrantMutex);
        assertTrue(reentrantMutex.isHeld());
        assertTrue(otherReentrantMutex.isHeld());
        assertEquals(fixture.expectedResult, reentrantMutex == otherReentrantMutex);

        //teardown
        reentrantMutex.release();
        otherReentrantMutex.release();
    }

    private static Stream<Fixture> lockerValuePairsProvider() {
        TestReentrantLocker testLocker = new TestReentrantLocker();
        TestReentrantLocker otherTestLocker = new TestReentrantLocker();
        return Stream.of(fixture(pair(testLocker, VALUE_A), pair(testLocker, VALUE_A), true),
                         fixture(pair(testLocker, VALUE_A), pair(testLocker, VALUE_A_OTHER_INSTANCE), true),
                         fixture(pair(testLocker, VALUE_A), pair(otherTestLocker, VALUE_A), true),
                         fixture(pair(testLocker, VALUE_A), pair(otherTestLocker, VALUE_A_OTHER_INSTANCE), true),
                         fixture(pair(testLocker, VALUE_1), pair(otherTestLocker, VALUE_1), true),
                         fixture(pair(testLocker, VALUE_1), pair(otherTestLocker, VALUE_1_OTHER_INSTANCE), true),
                         fixture(pair(testLocker, VALUE_A), pair(testLocker, VALUE_B), false),
                         fixture(pair(testLocker, VALUE_A), pair(otherTestLocker, VALUE_B), false),
                         fixture(pair(testLocker, VALUE_1), pair(testLocker, VALUE_2), false),
                         fixture(pair(testLocker, VALUE_1), pair(otherTestLocker, VALUE_2), false),
                         fixture(pair(testLocker, VALUE_A), pair(otherTestLocker, VALUE_1), false)
        );
    }

    @Test
    void shouldAllowReentryForSameLockOnSameThread() {
        //given
        List<Object> sameValuesToLock = Arrays.asList(VALUE_A, VALUE_A, VALUE_A_OTHER_INSTANCE);
        List<TestReentrantMutex> mutexes = sameValuesToLock.stream()
                                                           .map(v -> locker.lock(v))
                                                           .collect(toList());
        TestReentrantMutex mutex = mutexes.get(0);

        int entrances = sameValuesToLock.size();

        //then
        assertActive(mutex, entrances);

        //then
        mutex.release();
        assertActive(mutex, --entrances);

        //then
        mutex.release();
        assertActive(mutex, --entrances);

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



    private static Fixture fixture(LockerAndValue lockerAndValue, LockerAndValue otherLockerAndValue, boolean expectedResult) {
        return new Fixture(lockerAndValue, otherLockerAndValue, expectedResult);
    }


    private static LockerAndValue pair(TestReentrantLocker locker, Object value) {
        return new LockerAndValue(locker, value);
    }

    //TODO could refactor to base class
    private void assertActive(TestReentrantMutex mutex, int entranceCount) {
        assertTrue(TestReentrantMutex.isActiveMutex(mutex.getLock()));
        assertTrue(mutex.isHeldByCurrentThread());
        assertEquals(entranceCount, mutex.getEntranceCount());
    }

    private void assertNotActive(TestReentrantMutex mutex) {
        assertFalse(TestReentrantMutex.isActiveMutex(mutex.getLock()));
        assertFalse(mutex.isHeld());
        assertTrue(mutex.noEntrances());
        //TODO remove but add to concurrent tests
        assertTrue(mutex.noWaitingThreads());
    }

    private static class Fixture {
        private LockerAndValue left;
        private LockerAndValue right;
        private boolean expectedResult;

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
