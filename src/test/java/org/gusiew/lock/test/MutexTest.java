package org.gusiew.lock.test;

import org.gusiew.lock.api.Locker;
import org.gusiew.lock.api.Mutex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.gusiew.lock.api.Mutex.withRunnable;
import static org.gusiew.lock.api.Mutex.withThrowsRunnable;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

class MutexTest {

    private static final Object SAMPLE = "SAMPLE";

    final private Locker locker = mock(Locker.class);
    final private Mutex mutex = mock(Mutex.class);

    private final Exception expectedException = new Exception();
    private final RuntimeException expectedRuntimeException = new RuntimeException();
    private Exception exceptionThrown = null;

    @BeforeEach
    void setupLocker() {
        when(locker.lock(any())).thenReturn(mutex);
    }

    @Test
    void withRunnableCompletesSuccessfully() throws Exception {
        //when
        withRunnable(locker.lock(SAMPLE), () -> {});
        //then
        verify(mutex, times(one())).release();
    }

    @Test
    void withThrowsRunnableCompletesSuccessfully() throws Exception {
        //when
        withThrowsRunnable(locker.lock(SAMPLE), () -> {});
        //then
        verify(mutex, times(one())).release();
    }

    @Test
    void withRunnableThrows() {
        //when
        executeRunnableThrowScenario();
        //then
        assertThrowScenario(expectedRuntimeException);
    }

    @Test
    void withThrowsRunnableThrows() {
        //when
        executeThrowsRunnableThrowScenario();
        //then
        assertThrowScenario(expectedException);
    }

    //TODO Dynamic tests to the rescue ?

    private void executeThrowsRunnableThrowScenario() {
        try {
            withThrowsRunnable(locker.lock(SAMPLE), () -> {throw expectedException;});
            fail("Should throw");
        } catch(Exception e) {
            exceptionThrown = e;
        }
    }

    private void executeRunnableThrowScenario() {
        try {
            withRunnable(locker.lock(SAMPLE), () -> {throw expectedRuntimeException;});
            fail("Should throw");
        } catch(Exception e) {
            exceptionThrown = e;
        }
    }

    private void assertThrowScenario(Exception expected) {
        verify(mutex, times(one())).release();
        assertEquals(expected, exceptionThrown);
    }

    private int one() {
        return 1;
    }
}
