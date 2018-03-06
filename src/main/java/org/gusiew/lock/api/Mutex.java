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
     * See @link Locker}
     *
     * @throws Exception propagated exception
     */
    static void using(Mutex mutex, MutexRunnable mutexRunnable) throws Exception {
        try {
            mutexRunnable.run();
        } finally {
            mutex.release();
        }
    }

    /**
     * Runnable that throws Exception
     */
    @FunctionalInterface
    interface MutexRunnable {
        void run() throws Exception;
    }
}
