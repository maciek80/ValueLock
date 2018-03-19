package org.gusiew.lock.impl.exception;

import org.gusiew.lock.impl.ReentrantMutex;

/**
 * Thrown by {@link ReentrantMutex#release()}. See {@link ReentrantMutex#release()} for more details
 */
public class MutexNotActiveException extends MutexException {
}
