package org.gusiew.lock.impl.util;

import org.gusiew.lock.api.Locker;
import org.gusiew.lock.api.Mutex;
import org.gusiew.lock.impl.ReentrantLocker;
import org.gusiew.lock.impl.ReentrantMutex;
import org.gusiew.lock.impl.internal.ActiveMutexesUpdatedHandler;
import org.gusiew.lock.impl.internal.MutexFactory;
import org.gusiew.lock.test.util.ReflectionUtil;
import org.gusiew.lock.test.util.ScenarioThread;
import org.gusiew.lock.util.StripedMap;

public class TestReentrantLocker implements Locker {

    private static final String MUTEX_FACTORY_FIELD_NAME = "mutexFactory";
    private static final String ACTIVE_MUTEXES_UPDATED_HANDLER_FIELD_NAME = "activeMutexesUpdatedHandler";
    private static final String LOCKS_FIELD_NAME = "locks";

    private final ReentrantLocker reentrantLocker;

    public TestReentrantLocker(ReentrantLocker reentrantLocker) {
        this.reentrantLocker = reentrantLocker;
        setupTestMutexFactoryOnReentrantLocker();
        setupTestActiveMutexesUpdatedHandlerOnReentrantLocker();
    }

    private void setupTestMutexFactoryOnReentrantLocker() {
        MutexFactory factory = ReflectionUtil.getValue(reentrantLocker, MUTEX_FACTORY_FIELD_NAME, MutexFactory.class);
        ReflectionUtil.setValue(reentrantLocker, MUTEX_FACTORY_FIELD_NAME, createTestReentrantMutexFactory(factory));
    }

    private void setupTestActiveMutexesUpdatedHandlerOnReentrantLocker() {
        ActiveMutexesUpdatedHandler handler = ReflectionUtil.getValue(reentrantLocker, ACTIVE_MUTEXES_UPDATED_HANDLER_FIELD_NAME, ActiveMutexesUpdatedHandler.class);
        ReflectionUtil.setValue(reentrantLocker, ACTIVE_MUTEXES_UPDATED_HANDLER_FIELD_NAME, createActiveMutexesUpdatedHandler(handler));
    }

    @Override
    public TestReentrantMutex lock(Object value) {
        return (TestReentrantMutex) reentrantLocker.lock(value);
    }


    private MutexFactory createTestReentrantMutexFactory(MutexFactory factory) {
        return value -> {
            ReentrantMutex reentrantMutex = factory.createAndLock(value);
            return TestReentrantMutex.from(reentrantMutex);
        };
    }

    private ActiveMutexesUpdatedHandler createActiveMutexesUpdatedHandler(ActiveMutexesUpdatedHandler handler) {
        return () -> {
            ScenarioThread.ThreadLocalContext c = ScenarioThread.getThreadContext();
            if (c.getOptions().isSuspendDuringLocking()) {
                c.getSuspender().suspend();
            } else {
                handler.activeMutexesUpdated();
            }
        };
    }

    public int getConcurrencyLevel() {
        return getLocks().getNumberOfStripes();
    }

    public TestReentrantMutex getFromActiveMutexes(Object lock) {
        TestReentrantMutex mutex;
        StripedMap<Object, Mutex> stripedMap = getLocks();
        synchronized (stripedMap.getStripe(lock)) {
            mutex = (TestReentrantMutex) stripedMap.get(lock);
            if(mutex != null) {
                mutex = mutex.synchronizedSelfGet();
            }
        }
        return mutex;
    }

    public boolean isActiveMutex(Object value) {
        return getFromActiveMutexes(value) != null;
    }

    public boolean activeMutexesEmpty() {

        StripedMap<Object, Mutex> stripedMap = getLocks();
        return internalCheckEmpty(stripedMap, 0);
    }

    private boolean internalCheckEmpty(StripedMap<Object, Mutex> stripedMap, int fromStripeIndex) {
        if(fromStripeIndex < stripedMap.getNumberOfStripes()) {
            synchronized (stripedMap.getStripe(fromStripeIndex)) {
                return internalCheckEmpty(stripedMap, fromStripeIndex + 1);
            }
        }
        return stripedMap.isEmpty();
    }

    private StripedMap<Object, Mutex> getLocks() {
        //TODO Generics
        return ReflectionUtil.getValue(reentrantLocker, LOCKS_FIELD_NAME, StripedMap.class);
    }
}