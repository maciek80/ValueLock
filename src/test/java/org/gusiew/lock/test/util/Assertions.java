package org.gusiew.lock.test.util;

import org.gusiew.lock.impl.TestReentrantMutex;

import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.jupiter.api.Assertions.*;

public class Assertions {
    //TODO Consider improvements to make API more consistent and consize
    public static void assertActive(TestReentrantMutex mutex, int entranceCount) {
        assertTrue(TestReentrantMutex.isActiveMutex(mutex.getLock()));
        assertTrue(mutex.isHeldByCurrentThread());
        assertEquals(entranceCount, mutex.getEntranceCount());
    }

    public static void assertNotActive(TestReentrantMutex mutex) {
        assertFalse(TestReentrantMutex.isActiveMutex(mutex.getLock()));
        assertFalse(mutex.isHeld());
        assertTrue(mutex.noEntrances());
    }

    public static ScenarioThread assertHoldsActiveMutex(List<ScenarioThread> scenarioThreads, String value) {
        TestReentrantMutex activeMutex = TestReentrantMutex.getFromActiveMutexes(value);
        List<ScenarioThread> activeThreads = scenarioThreads.stream().filter(t -> t.equals(activeMutex.getHolderThread())).collect(toList());
        assertEquals(1, activeThreads.size());
        return activeThreads.get(0);
    }

    public static void assertThreadHanging(ScenarioThread thread) {
        assertFalse(thread.isSuspended());
        assertTrue(thread.isAlive());
    }

    public static void assertMutexNotActive(Object value) {
        assertNull(TestReentrantMutex.getFromActiveMutexes(value));
    }

    public static void assertMutexActiveButNotHeld(Object value) {
        TestReentrantMutex activeMutex = TestReentrantMutex.getFromActiveMutexes(value);
        assertNotNull(activeMutex);
        assertNull(activeMutex.getHolderThread());
    }

    public static void assertThreadHoldsActiveMutex(ScenarioThread thread, Object value) {
        TestReentrantMutex threadMutex = thread.getMutex(value);
        TestReentrantMutex activeMutex = TestReentrantMutex.getFromActiveMutexes(value);

        assertSame(threadMutex, activeMutex);
        assertNotNull(threadMutex);
        assertEquals(thread, threadMutex.getHolderThread());
    }

    public static void assertThreadDidNotObtainMutex(ScenarioThread thread, Object value) {
        TestReentrantMutex threadMutex = thread.getMutex(value);
        assertNull(threadMutex);
    }

    public static void assertNoWaitingThreads(Object value) {
        assertWaitingThreads(value, 0);
    }

    public static void assertWaitingThreads(Object value, int numberOfThreads) {
        TestReentrantMutex activeMutex = TestReentrantMutex.getFromActiveMutexes(value);
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
