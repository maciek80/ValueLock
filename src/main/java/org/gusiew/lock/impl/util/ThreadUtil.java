package org.gusiew.lock.impl.util;

public class ThreadUtil {
    public static boolean sameThreads(Thread thread1, Thread thread2) {
        return thread1.equals(thread2);
    }
}
