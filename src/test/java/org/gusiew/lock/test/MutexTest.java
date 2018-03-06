package org.gusiew.lock.test;

import org.gusiew.lock.api.Locker;
import org.gusiew.lock.api.Mutex;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.gusiew.lock.api.Mutex.using;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

class MutexTest {

    private static final Object SAMPLE = "SAMPLE";

    private Mutex.MutexRunnable runnable = mock(Mutex.MutexRunnable.class);

    private Locker locker = mock(Locker.class);
    private Mutex mutex = mock(Mutex.class);

    private MyException expectedException = new MyException();
    private Exception exceptionThrown = null;

    @BeforeEach
    void setupLocker() {
        when(locker.lock(any())).thenReturn(mutex);
    }

    @Test
    void usingRunnableCompletesSuccessfully() throws Exception {
        //when
        using(locker.lock(SAMPLE), () -> {});
        //then
        verify(mutex, times(1)).release();
    }

    @Test
    void usingRunnableThrows() {
        //given
        expectThrow(runnable, expectedException);
        //when
        executeThrowScenario();
        //then
        verify(mutex, times(1)).release();
        assertEquals(expectedException, exceptionThrown);
    }

    private void expectThrow(Mutex.MutexRunnable runnable, MyException expectedException) {
        try {
            doThrow(expectedException).when(runnable).run();
        } catch (Exception e) {
            fail("Should not throw when mocking");
        }
    }

    private void executeThrowScenario() {
        try {
            using(locker.lock(SAMPLE), runnable);
            fail("Should throw");
        } catch(Exception e) {
            exceptionThrown = e;
        }
    }

    private class MyException extends Exception {}
}
