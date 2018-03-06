package org.gusiew.lock.test;

import org.gusiew.lock.impl.TestReentrantLocker;

class AbstractReentrantLockerTest {

    static final String VALUE_A = "A";
    final static String VALUE_B = "B";
    final static String VALUE_C = "C";

    TestReentrantLocker locker = new TestReentrantLocker();
}
