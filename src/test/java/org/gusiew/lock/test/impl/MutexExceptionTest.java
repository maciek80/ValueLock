package org.gusiew.lock.test.impl;

import org.gusiew.lock.impl.exception.MutexActiveButDifferent;
import org.gusiew.lock.impl.exception.MutexException;
import org.gusiew.lock.impl.exception.MutexHeldByOtherThreadException;
import org.gusiew.lock.impl.exception.MutexNotActiveException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class MutexExceptionTest {
    @Test
    void shouldBeMutexException() {
        assertTrue(new MutexActiveButDifferent() instanceof MutexException);
        assertTrue(new MutexHeldByOtherThreadException() instanceof MutexException);
        assertTrue(new MutexNotActiveException() instanceof MutexException);
    }
}
