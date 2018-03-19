package org.gusiew.lock.test.impl;

import org.gusiew.lock.impl.ReentrantLocker;
import org.gusiew.lock.test.impl.util.Assertions;
import org.gusiew.lock.test.impl.util.TestReentrantLocker;
import org.junit.jupiter.api.AfterEach;

class AbstractReentrantLockerTest {

    static final String VALUE_A = "A";
    static final String VALUE_A_OTHER_INSTANCE = new String(VALUE_A);
    static final String VALUE_B = "B";
    static final String VALUE_C = "C";
    TestReentrantLocker locker = new TestReentrantLocker(new ReentrantLocker());

    @AfterEach
    void checkActiveMutexesEmpty() {
        Assertions.assertActiveMutexesEmpty(locker);
    }
}
