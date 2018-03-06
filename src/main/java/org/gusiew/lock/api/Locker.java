package org.gusiew.lock.api;

/**
 * Lock service that locks on value instead of reference
 * <p>If two objects are equal but not necessarily same instances the lock service synchronizes on same mutex
 * <p>Example
 *   <pre>
 *      thread A: valueLocker.lock(new String("A"));
 *      thread B: valueLocker.lock("A")
 *
 *      Thread A and B compete for same mutex
 *   </pre>
 *   <p>Usages
 *  Functional style with code that does not throw checked exceptions
 *  <pre> {@code
 *     Locker l = new ReentrantLocker();
 *     withRunnable(l.lock(value), () -> {...})
 *  }
 *  </pre>
 *  Functional style with code that throws checked exceptions
 *  <pre> {@code
 *     Locker l = new ReentrantLocker();
 *     withThrowsRunnable(l.lock(value), () -> {...})
 *  }
 *  </pre>
 *  Imperative style
 *  <pre> {@code
 *   try {
 *       ...
 *   } finally {
 *     m.release();
 *   }
 */
public interface Locker {

    /**
     * Implementations should lock on value equality. Caller is responsible for releasing the lock (see class comments)
     *
     */
    Mutex lock(Object value);

}
