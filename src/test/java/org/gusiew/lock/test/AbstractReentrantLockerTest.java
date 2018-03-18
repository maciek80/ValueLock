package org.gusiew.lock.test;

import org.gusiew.lock.impl.TestReentrantLocker;
import org.junit.jupiter.api.AfterEach;

import static org.gusiew.lock.test.util.Assertions.assertActiveMutexesEmpty;

class AbstractReentrantLockerTest {

    static final String VALUE_A = "A";
    static final String VALUE_A_OTHER_INSTANCE = new String(VALUE_A);
    static final String VALUE_B = "B";
    static final String VALUE_C = "C";
    TestReentrantLocker locker = new TestReentrantLocker();

    @AfterEach
    void checkActiveMutexesEmpty() {
        assertActiveMutexesEmpty(locker);
    }
}
