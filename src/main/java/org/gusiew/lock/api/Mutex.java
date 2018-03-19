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
     * @param mutex mutex that should be released when runnable is completed or runtime exception is thrown
     * @param runnable runnable to execute, may throw runtime exceptions
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
     * @param mutex mutex that should be released when runnable is completed or exception is thrown
     * @param throwsRunnable throwsRunnable to execute, may throw runtime and checked exceptions
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
