/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache license, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */
package com.johnsoft.listeners.dispatcher;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.johnsoft.listeners.Callback;
import com.johnsoft.listeners.Listener;
import com.johnsoft.listeners.ListenerExecutor;
import com.johnsoft.listeners.ListenerFoundation;
import com.johnsoft.listeners.executors.AbstractListenerExecutor;

/**
 * callback dispatcher [all Callback are independent, all Callback will be called]
 *
 * @author John Kenrinus Lee
 * @version 2016-07-15
 */
public class CallbackDispatcher<E> extends AbstractDispatcher<E> {
    private final Map<Callback<E>, ListenerExecutor> callbackExecutorMap;
    private final Map<Class<E>, ListenerExecutor> eventClassExecutorMap;
    private final Map<Class<E>, Class<ListenerExecutor>> eventClassExecutorClassMap;
    private final Class<ListenerExecutor> perCallbackExecutorClass;
    private final ListenerExecutor defaultExecutor;
    private final ListenerExecutor destroyExecutor;

    public CallbackDispatcher() {
        this(new Builder<E>());
    }

    protected CallbackDispatcher(Builder<E> builder) {
        super(builder);
        callbackExecutorMap = builder.callbackExecutorMapBak;
        eventClassExecutorMap = builder.eventClassExecutorMapBak;
        eventClassExecutorClassMap = builder.eventClassExecutorClassMap;
        perCallbackExecutorClass = builder.perCallbackExecutorClass;
        defaultExecutor = builder.defaultExecutor;
        destroyExecutor = builder.destroyExecutor;
    }

    @Override
    public Builder<E> newBuilder() {
        return new Builder<>(this);
    }

    @Override
    protected void onListenersUpdate(int code, Listener<E> listener) {
        switch (code) {
            case CODE_LISTENER_ADDED:
                if (!(listener instanceof Callback)) {
                    getFoundation().throwException(new IllegalStateException("The listener "
                            + "added require Callback"));
                    return;
                }
                try {
                    onCallbackAdded((Callback<E>) listener);
                } catch (Throwable e) {
                    getFoundation().throwException(new IllegalStateException(e));
                    return;
                }
                break;
            case CODE_LISTENER_REMOVED:
                try {
                    onCallbackRemoved((Callback<E>) listener);
                } catch (Throwable e) {
                    getFoundation().throwException(new IllegalStateException(e));
                    return;
                }
                break;
            case CODE_LISTENERS_CLEARED:
                try {
                    onCallbacksCleared();
                } catch (Throwable e) {
                    getFoundation().throwException(new IllegalStateException(e));
                    return;
                }
                break;
            default:
                getFoundation().throwException(new UnsupportedOperationException("Unknown listeners "
                        + "update code"));
        }
    }

    @Override
    protected void doNotifyListeners(Collection<Listener<E>> listeners, E event) {
        for (Listener<E> listener : listeners) {
            try {
                notifyCallback((Callback<E>) listener, event);
            } catch (Throwable e) {
                getFoundation().catchThrowable(e);
            }
        }
    }

    private void onCallbackAdded(Callback<E> callback) throws Throwable {
        ListenerExecutor listenerExecutor = callbackExecutorMap.get(callback);
        if (listenerExecutor != null
                && AbstractListenerExecutor.isNotInitialized(listenerExecutor.getState())) {
            listenerExecutor.initialize(); // sync
            return;
        }
        if (perCallbackExecutorClass != null) {
            listenerExecutor = perCallbackExecutorClass.newInstance();
            listenerExecutor.initialize(); // sync
            callbackExecutorMap.put(callback, listenerExecutor);
        }
    }

    private void onCallbackRemoved(Callback<E> callback) throws Throwable {
        destroyCallbackBoundExecutor(callbackExecutorMap.remove(callback));
    }

    private void onCallbacksCleared() throws Throwable {
        for (Callback<E> callback : callbackExecutorMap.keySet()) {
            destroyCallbackBoundExecutor(callbackExecutorMap.remove(callback));
        }
    }

    private void destroyCallbackBoundExecutor(final ListenerExecutor listenerExecutor) {
        if (listenerExecutor != null) {
            if (!AbstractListenerExecutor.isDestroyed(listenerExecutor.getState())) {
                // TODO if the ListenerExecutor for two more Callback, should we destroy it now or lazy?
                if (destroyExecutor != null) {
                    destroyExecutor.execute(new NoncancelabilityTask(getFoundation()) {
                        @Override
                        public void doTask() {
                            listenerExecutor.destroy();
                        }
                    });
                } else {
                    listenerExecutor.destroy();
                }
            }
        }
    }

    private void notifyCallback(final Callback<E> callback, final E event) throws Throwable {
        final NoncancelabilityTask task = new NoncancelabilityTask(getFoundation()) {
            @Override
            public void doTask() {
                try {
                    callback.on(event);
                } catch (Throwable e) {
                    getFoundation().catchThrowable(e);
                }
            }
        };
        ListenerExecutor listenerExecutor;

        listenerExecutor = callbackExecutorMap.get(callback);
        if (listenerExecutor != null) {
            listenerExecutor.execute(task);
            return;
        }

        @SuppressWarnings("unchecked")
        final Class<E> eventClass = (Class<E>) event.getClass();

        listenerExecutor = eventClassExecutorMap.get(eventClass);
        if (listenerExecutor != null) {
            listenerExecutor.execute(task);
            return;
        }

        if (eventClassExecutorClassMap != null) {
            Class<ListenerExecutor> executorClass = eventClassExecutorClassMap.get(eventClass);
            if (executorClass != null) {
                listenerExecutor = executorClass.newInstance();
                listenerExecutor.initialize(); // sync
                eventClassExecutorMap.put(eventClass, listenerExecutor);
                listenerExecutor.execute(task);
            }
        }

        if (defaultExecutor != null) {
            defaultExecutor.execute(task);
            return;
        }

        task.run();
    }

    // If the user forgets destroy listener executors, try to tip a last effort here.
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        try {
            if (defaultExecutor != null) {
                defaultExecutor.on(this + "[defaultExecutor]#finalize");
            }
            if (destroyExecutor != null && destroyExecutor != defaultExecutor) {
                destroyExecutor.on(this + "[destroyExecutor]#finalize");
            }
        } catch (Throwable e) {
            // ignored
        }
    }

    public static class Builder<E> extends AbstractDispatcher.Builder<E> {
        private final Map<Callback<E>, ListenerExecutor> callbackExecutorMapBak;
        private final Map<Class<E>, ListenerExecutor> eventClassExecutorMapBak;

        private Map<Class<E>, Class<ListenerExecutor>> eventClassExecutorClassMap;
        private Class<ListenerExecutor> perCallbackExecutorClass;
        private ListenerExecutor defaultExecutor;
        private ListenerExecutor destroyExecutor;

        public Builder() {
            super();
            callbackExecutorMapBak = Collections.synchronizedMap(new HashMap<Callback<E>, ListenerExecutor>());
            eventClassExecutorMapBak = Collections.synchronizedMap(new HashMap<Class<E>, ListenerExecutor>());
            eventClassExecutorClassMap = null;
            perCallbackExecutorClass = null;
            defaultExecutor = null;
            destroyExecutor = null;
        }

        protected Builder(CallbackDispatcher<E> dispatcher) {
            super(dispatcher);
            callbackExecutorMapBak = dispatcher.callbackExecutorMap;
            eventClassExecutorMapBak = dispatcher.eventClassExecutorMap;
            eventClassExecutorClassMap = dispatcher.eventClassExecutorClassMap;
            perCallbackExecutorClass = dispatcher.perCallbackExecutorClass;
            defaultExecutor = dispatcher.defaultExecutor;
            destroyExecutor = dispatcher.destroyExecutor;
        }

        public Map<Callback<E>, ListenerExecutor> getCallbackExecutorMapBak() {
            return callbackExecutorMapBak;
        }

        public Map<Class<E>, ListenerExecutor> getEventClassExecutorMapBak() {
            return eventClassExecutorMapBak;
        }

        public Map<Class<E>, Class<ListenerExecutor>> getEventClassExecutorClassMap() {
            return eventClassExecutorClassMap;
        }

        /** If each type of event need special executor, this method will be helpful. */
        public CallbackDispatcher.Builder<E> setEventClassExecutorClassMap(
                Map<Class<E>, Class<ListenerExecutor>> eventClassExecutorClassMap) {
            this.eventClassExecutorClassMap = eventClassExecutorClassMap;
            return this;
        }

        public Class<ListenerExecutor> getPerCallbackExecutorClass() {
            return perCallbackExecutorClass;
        }

        /** If not {@code null}, each Callback will be notified on own executor(using reflect create) */
        public CallbackDispatcher.Builder<E> setPerCallbackExecutorClass(Class<ListenerExecutor> perCallbackExecutorClass) {
            this.perCallbackExecutorClass = perCallbackExecutorClass;
            return this;
        }

        public ListenerExecutor getDefaultExecutor() {
            return defaultExecutor;
        }

        /** If not {@code null}, and all listeners should calling on one thread, will schedule on it */
        public CallbackDispatcher.Builder<E> setDefaultExecutor(ListenerExecutor defaultExecutor) {
            this.defaultExecutor = defaultExecutor;
            return this;
        }

        public ListenerExecutor getDestroyExecutor() {
            return destroyExecutor;
        }

        /** The ListenerExecutor which use for destroy executor useless. */
        public CallbackDispatcher.Builder<E> setDestroyExecutor(ListenerExecutor destroyExecutor) {
            this.destroyExecutor = destroyExecutor;
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public CallbackDispatcher.Builder<E> setFoundation(ListenerFoundation foundation) {
            super.setFoundation(foundation);
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public CallbackDispatcher.Builder<E> setDistinctApplied(boolean distinctApplied) {
            super.setDistinctApplied(distinctApplied);
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public CallbackDispatcher.Builder<E> setWeakReferenceApplied(boolean weakReferenceApplied) {
            super.setWeakReferenceApplied(weakReferenceApplied);
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public CallbackDispatcher.Builder<E> setPriorityApplied(boolean priorityApplied) {
            super.setPriorityApplied(priorityApplied);
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public CallbackDispatcher.Builder<E> setNotifyThread(ListenerExecutor notifyThread) {
            super.setNotifyThread(notifyThread);
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public CallbackDispatcher.Builder<E> setVisitThread(ListenerExecutor visitThread) {
            super.setVisitThread(visitThread);
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public CallbackDispatcher.Builder<E> setPriorityComparator(Comparator<Listener<E>> priorityComparator) {
            super.setPriorityComparator(priorityComparator);
            return this;
        }

        @Override
        public CallbackDispatcher<E> build() {
            return new CallbackDispatcher<>(this);
        }
    }
}
