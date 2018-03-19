package org.gusiew.lock.test.impl.util;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

public class Assertions {
    //TODO Consider improvements to make API more consistent and concise, perhaps bundle with driver (fluent API)

    public static void assertActiveAndHeldByCurrentThread(TestReentrantLocker locker, TestReentrantMutex mutex) {
        assertTrue(locker.isActiveMutex(mutex.getLock()));
        assertTrue(mutex.isHeldByCurrentThread());
    }

    public static void assertActiveAndHeldByCurrentThreadWithEntrances(TestReentrantLocker locker, TestReentrantMutex mutex, int entranceCount) {
        assertActiveAndHeldByCurrentThread(locker, mutex);
        assertEquals(entranceCount, mutex.getEntranceCount());
    }

    public static void assertNotActive(TestReentrantLocker locker, TestReentrantMutex mutex) {
        assertFalse(locker.isActiveMutex(mutex.getLock()));
        assertFalse(mutex.isHeld());
        assertTrue(mutex.noEntrances());
    }

    public static ScenarioThread assertHoldsActiveMutex(TestReentrantLocker locker, List<ScenarioThread> scenarioThreads, String value) {
        TestReentrantMutex activeMutex = locker.getFromActiveMutexes(value);
        List<ScenarioThread> activeThreads = scenarioThreads.stream().filter(t -> t.equals(activeMutex.getHolderThread())).collect(toList());
        assertEquals(1, activeThreads.size());
        return activeThreads.get(0);
    }

    public static void assertThreadHanging(ScenarioThread thread) {
        assertFalse(thread.isSuspended());
        assertTrue(thread.isAlive());
    }

    public static void assertMutexNotActive(TestReentrantLocker locker, Object value) {
        assertNull(locker.getFromActiveMutexes(value));
    }

    public static void assertActiveMutexesEmpty(TestReentrantLocker locker) {
        assertTrue(locker.activeMutexesEmpty());
    }

    public static void assertMutexActiveButNotHeld(TestReentrantLocker locker, Object value) {
        TestReentrantMutex activeMutex = locker.getFromActiveMutexes(value);
        assertNotNull(activeMutex);
        assertNull(activeMutex.getHolderThread());
    }

    public static void assertThreadHoldsActiveMutex(TestReentrantLocker locker, ScenarioThread thread, Object value) {
        TestReentrantMutex threadMutex = thread.getMutex(value);
        TestReentrantMutex activeMutex = locker.getFromActiveMutexes(value);

        assertSame(threadMutex, activeMutex);
        assertNotNull(threadMutex);
        assertEquals(thread, threadMutex.getHolderThread());
    }

    public static void assertThreadDidNotObtainMutex(ScenarioThread thread, Object value) {
        TestReentrantMutex threadMutex = thread.getMutex(value);
        assertNull(threadMutex);
    }

    public static void assertNoWaitingThreads(TestReentrantLocker locker, Object value) {
        assertWaitingThreads(locker, value, 0);
    }

    public static void assertWaitingThreads(TestReentrantLocker locker, Object value, int numberOfThreads) {
        TestReentrantMutex activeMutex = locker.getFromActiveMutexes(value);
        assertNotNull(activeMutex);
        assertEquals(numberOfThreads, activeMutex.getWaitingThreadsCount());
    }

    public static void assertThreadCompleted(ScenarioThread thread) {
        assertTrue(thread.completedSuccessfully());
        assertFalse(thread.isAlive());
    }

    public static void assertThreadSuspended(ScenarioThread thread) {
        assertTrue(thread.isSuspended());
    }
}
