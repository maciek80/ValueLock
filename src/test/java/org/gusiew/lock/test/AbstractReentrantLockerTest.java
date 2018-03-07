package org.gusiew.lock.test;

import org.gusiew.lock.impl.TestReentrantLocker;

class AbstractReentrantLockerTest {

    static final String VALUE_A = "A";
    static final String VALUE_A_OTHER_INSTANCE = new String(VALUE_A);
    static final String VALUE_B = "B";
    static final String VALUE_C = "C";

    TestReentrantLocker locker = new TestReentrantLocker();
}
