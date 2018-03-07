package org.gusiew.lock.impl.exception;

import org.gusiew.lock.impl.ReentrantMutex;

/**
 * @see ReentrantMutex#release().
 */
public class MutexActiveButDifferent extends RuntimeException {
}
