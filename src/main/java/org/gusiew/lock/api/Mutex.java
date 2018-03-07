package org.gusiew.lock.api;

/**
 * Represents lock acquired by {@link Locker}. Client needs to release the lock
 */
public interface Mutex {

    /**
     * Releases lock acquired by {@link Locker}. See {@link Locker} for more info
     */
    void release();

    /**
     * @see Locker
     *
     */
    static void withRunnable(Mutex mutex, Runnable runnable) {
        try {
            runnable.run();
        } finally {
            mutex.release();
        }
    }

    /**
     * @see Locker
     *
     * @throws Exception propagated exception
     */
    static void withThrowsRunnable(Mutex mutex, ThrowsRunnable throwsRunnable) throws Exception {
        try {
            throwsRunnable.run();
        } finally {
            mutex.release();
        }
    }

    /**
     * Runnable that throws Exception
     *
     * @see Locker
     */
    @FunctionalInterface
    interface ThrowsRunnable {
        void run() throws Exception;
    }
}
