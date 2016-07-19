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
package com.johnsoft.listeners;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

/**
 * @author John Kenrinus Lee
 * @version 2016-07-15
 */
public class CallbackDispatcher<E> extends AbstractDispatcher<E> {
    private static <E> Map<Callback<E>, ListenerExecutor> generateCallbackListenerExecutorMap() {
        return Collections.synchronizedMap(new HashMap<Callback<E>, ListenerExecutor>());
    }

    private final WeakHashMap<ListenerExecutor, byte[]> discardExecutors = new WeakHashMap<>();
    private final byte[] content = new byte[0];

    private final Map<Callback<E>, ListenerExecutor> customExecutorMap;
    private final Class<? extends ListenerExecutor> perExecutorClass;
    private final ListenerExecutor defaultExecutor;
    private final ListenerExecutor destroyExecutor;

    public CallbackDispatcher() {
        this(new Builder<E>());
    }

    protected CallbackDispatcher(Builder<E> builder) {
        super(builder);
        if (builder.customExecutorMap != null) {
            customExecutorMap = builder.customExecutorMap;
        } else {
            customExecutorMap = generateCallbackListenerExecutorMap();
        }
        perExecutorClass = builder.perExecutorClass;
        defaultExecutor = builder.defaultExecutor;
        destroyExecutor = builder.destroyExecutor;
    }

    @Override
    public Builder<E> newBuilder() {
        return new Builder<>(this);
    }

    public final ListenerExecutor getDefaultExecutor() {
        return defaultExecutor;
    }

    public final ListenerExecutor getDestroyExecutor() {
        return destroyExecutor;
    }

    @Override
    protected void onListenersUpdate(int code, Listener<E> listener) {
        switch (code) {
            case CODE_LISTENER_ADDED:
                try {
                    // try cast to report error at addListener
                    final Callback<E> callback = (Callback<E>)listener;
                    System.out.println("add Callback: " + callback);
                    onCallbackAdded(callback);
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
                break;
            case CODE_LISTENER_REMOVED:
                try {
                    onCallbackRemoved((Callback<E>)listener);
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
                break;
            case CODE_LISTENERS_CLEARED:
                try {
                    onCallbacksCleared();
                } catch (Throwable e) {
                    throw new IllegalStateException(e);
                }
                break;
            default:
                throw new UnsupportedOperationException("Unknown listeners update code");
        }
    }

    private void onCallbackAdded(Callback<E> callback) throws Throwable {
        ListenerExecutor listenerExecutor = customExecutorMap.get(callback);
        if (listenerExecutor != null && listenerExecutor.isNotInitialized()) {
            listenerExecutor.initialize(); // sync
            return;
        }
        if (perExecutorClass != null) {
            listenerExecutor = perExecutorClass.newInstance();
            listenerExecutor.initialize(); // sync
            customExecutorMap.put(callback, listenerExecutor);
        }
    }

    private void onCallbackRemoved(Callback<E> callback) throws Throwable {
        destroyBoundListenerExecutor(customExecutorMap.remove(callback));
    }

    private void onCallbacksCleared() throws Throwable {
        final Set<Callback<E>> callbacks = customExecutorMap.keySet();
        for (Callback<E> callback : callbacks) {
            destroyBoundListenerExecutor(customExecutorMap.remove(callback));
        }
    }

    private void destroyBoundListenerExecutor(final ListenerExecutor listenerExecutor) {
        if (listenerExecutor != null) {
            if (listenerExecutor.getMode() == ListenerExecutor.Mode.FOR_SINGLE_LISTENER
                    && !listenerExecutor.isDestroyed()) {
                if (destroyExecutor != null) {
                    destroyExecutor.execute(new Runnable() {
                        @Override
                        public void run() {
                            listenerExecutor.destroy();
                        }
                    });
                } else {
                    listenerExecutor.destroy();
                }
            } else if (listenerExecutor.getMode() == ListenerExecutor.Mode.FOR_SINGLE_LISTENER
                    && !listenerExecutor.isDestroyed()
                    && perExecutorClass != null && listenerExecutor.getClass().equals(perExecutorClass)) {
                discardExecutors.put(listenerExecutor, content);
            }
        }
    }

    @Override
    protected void doNotifyListeners(Listener<E>[] listeners, E event) {
        for (int i = 0; i < listeners.length; ++i) {
            try {
                notifyCallback((Callback<E>) listeners[i], event);
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    private void notifyCallback(final Callback<E> callback, final E event) throws Throwable {
        final Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    callback.on(event);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
        };
        final ListenerExecutor listenerExecutor = customExecutorMap.get(callback);
        if (listenerExecutor != null) {
            listenerExecutor.execute(runnable);
            return;
        }
        if (defaultExecutor != null) {
            defaultExecutor.execute(runnable);
            return;
        }
        runnable.run();
    }

    // If the user forgets destroy listener executors, try to make a last effort here.
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        final Set<ListenerExecutor> listenerExecutors = discardExecutors.keySet();
        for (ListenerExecutor executor : listenerExecutors) {
            destroyBoundListenerExecutor(executor);
        }
        destroyBoundListenerExecutor(defaultExecutor);
        destroyBoundListenerExecutor(destroyExecutor);
    }

    private void destroySpecialExecutorSilently(ListenerExecutor listenerExecutor) {
        try {
            if (listenerExecutor != null && listenerExecutor.getMode() == ListenerExecutor.Mode.FOR_SINGLE_DISPATCHER
                    && !listenerExecutor.isDestroyed()) {
                listenerExecutor.destroy();
            }
        } catch (Throwable e) {
            // ignored
        }
    }

    public static class Builder<E> extends AbstractDispatcher.Builder<E> {
        private final Map<Callback<E>, ListenerExecutor> customExecutorMap;
        private Class<? extends ListenerExecutor> perExecutorClass;
        private ListenerExecutor defaultExecutor;
        private ListenerExecutor destroyExecutor;

        public Builder() {
            super();
            customExecutorMap = generateCallbackListenerExecutorMap();
            perExecutorClass = null;
            defaultExecutor = null;
            destroyExecutor = null;
        }

        protected Builder(CallbackDispatcher<E> dispatcher) {
            super(dispatcher);
            if (dispatcher.customExecutorMap != null) {
                customExecutorMap = dispatcher.customExecutorMap;
            } else {
                customExecutorMap = generateCallbackListenerExecutorMap();
            }
            perExecutorClass = dispatcher.perExecutorClass;
            defaultExecutor = dispatcher.defaultExecutor;
            destroyExecutor = dispatcher.destroyExecutor;
        }

        public final Map<Callback<E>, ListenerExecutor> customExecutorMap() {
            return customExecutorMap;
        }

        public Class<? extends ListenerExecutor> getPerExecutorClass() {
            return perExecutorClass;
        }

        public Builder<E> setPerExecutorClass(Class<? extends ListenerExecutor> perExecutorClass) {
            this.perExecutorClass = perExecutorClass;
            return this;
        }

        public ListenerExecutor getDefaultExecutor() {
            return defaultExecutor;
        }

        public Builder<E> setDefaultExecutor(ListenerExecutor defaultExecutor) {
            this.defaultExecutor = defaultExecutor;
            return this;
        }

        public ListenerExecutor getDestroyExecutor() {
            return destroyExecutor;
        }

        public Builder<E> setDestroyExecutor(ListenerExecutor destroyExecutor) {
            this.destroyExecutor = destroyExecutor;
            return this;
        }

        @Override
        public Builder<E> setDistinct(boolean distinct) {
            super.setDistinct(distinct);
            return this;
        }

        @Override
        public Builder<E> setCallThread(ListenerExecutor callThread) {
            super.setCallThread(callThread);
            return this;
        }

        @Override
        public Builder<E> setVisitSameWithNotify(boolean visitSameWithNotify) {
            super.setVisitSameWithNotify(visitSameWithNotify);
            return this;
        }

        @Override
        public CallbackDispatcher<E> build() {
            return new CallbackDispatcher<>(this);
        }
    }
}
