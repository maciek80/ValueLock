package org.gusiew.lock.test;

import org.gusiew.lock.impl.ReentrantLocker;
import org.gusiew.lock.impl.ReentrantMutex;
import org.gusiew.lock.impl.exception.MutexActiveButDifferent;
import org.gusiew.lock.impl.exception.MutexNotActiveException;
import org.gusiew.lock.impl.util.TestReentrantLocker;
import org.gusiew.lock.impl.util.TestReentrantMutex;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

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
    private static final int DEFAULT_CONCURRENCY_LEVEL = 16;

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
        assertActiveAndHeldByCurrentThread(locker, mutex);
        //teardown
        mutex.release();
    }

    @Test
    void shouldReleaseAcquiredMutex() {
        //when
        TestReentrantMutex mutex = locker.lock(VALUE_A);
        mutex.release();
        //then
        assertNotActive(locker, mutex);
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
        //when
        TestReentrantMutex reentrantMutex = locker.lock(fixture.value1);
        TestReentrantMutex otherReentrantMutex = locker.lock(fixture.value2);
        //then
        assertActiveAndHeldByCurrentThread(locker, reentrantMutex);
        assertActiveAndHeldByCurrentThread(locker, otherReentrantMutex);
        assertEquals(fixture.expectedResult, reentrantMutex == otherReentrantMutex);

        //teardown
        reentrantMutex.release();
        otherReentrantMutex.release();
    }

    private static Stream<Fixture> lockerValuePairsProvider() {
        return Stream.of(fixture( VALUE_A,  VALUE_A, true),
                         fixture( VALUE_A,  VALUE_A_OTHER_INSTANCE, true),
                         fixture( VALUE_1,  VALUE_1, true),
                         fixture( VALUE_1,  VALUE_1_OTHER_INSTANCE, true),
                         fixture( VALUE_A,  VALUE_B, false),
                         fixture( VALUE_A,  VALUE_B, false),
                         fixture( VALUE_1,  VALUE_2, false),
                         fixture( VALUE_1,  VALUE_2, false),
                         fixture( VALUE_A,  VALUE_1, false)
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
        assertActiveAndHeldByCurrentThreadWithEntrances(locker, mutex, entrances);

        //then
        mutex.release();
        assertActiveAndHeldByCurrentThreadWithEntrances(locker, mutex, --entrances);

        //then
        mutex.release();
        assertActiveAndHeldByCurrentThreadWithEntrances(locker, mutex, --entrances);

        //then
        mutex.release();
        assertNotActive(locker, mutex);
    }

    @Test
    void shouldReleaseInOtherOrder() {
        //given
        TestReentrantMutex mutexA = locker.lock(VALUE_A);
        TestReentrantMutex mutexB = locker.lock(VALUE_B);
        mutexB.release();
        mutexA.release();
        //then
        assertNotActive(locker, mutexA);
        assertNotActive(locker, mutexB);
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

    @Test
    void shouldApplyDefaultConcurrencyLevel() {
        assertEquals(DEFAULT_CONCURRENCY_LEVEL, locker.getConcurrencyLevel());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4})
    void shouldApplyCorrectConcurrencyLevel(int level) {
        assertEquals(level, new TestReentrantLocker(new ReentrantLocker(level)).getConcurrencyLevel());
    }

    private static Fixture fixture(Object value1, Object value2, boolean expectedResult) {
        return new Fixture(value1, value2, expectedResult);
    }

    private static class Fixture {
        final private Object value1;
        final private Object value2;
        final private boolean expectedResult;

        Fixture(Object value1, Object value2, boolean expectedResult) {
            this.value1 = value1;
            this.value2 = value2;
            this.expectedResult = expectedResult;
        }
    }
    
}
