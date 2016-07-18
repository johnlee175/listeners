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

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.LinkedList;

/**
 * @author John Kenrinus Lee
 * @version 2016-07-15
 */
public abstract class AbstractDispatcher implements ListenerDispatcher {
    protected static final int CODE_LISTENER_ADDED = 1;
    protected static final int CODE_LISTENERS_CLEARED = 0;
    protected static final int CODE_LISTENER_REMOVED = -1;

    private final byte[] listenersLock = new byte[0];
    private final Collection<Listener> listeners;
    private final boolean distinct;
    private final boolean visitSameWithNotify;
    private final ListenerExecutor callThread;

    protected AbstractDispatcher(Builder builder) {
        distinct = builder.distinct;
        visitSameWithNotify = builder.visitSameWithNotify;
        callThread = builder.callThread;
        if (distinct) {
            listeners = new LinkedHashSet<>();
        } else {
            listeners = new LinkedList<>();
        }
    }

    public abstract Builder newBuilder();

    public final ListenerExecutor getCallThread() {
        return callThread;
    }

    @Override
    public final boolean addListener(Listener listener) {
        if (listener != null) {
            synchronized(listenersLock) {
                final boolean result = listeners.add(listener);
                deliverListenersUpdate(CODE_LISTENER_ADDED, listener);
                return result;
            }
        }
        return false;
    }

    @Override
    public final boolean removeListener(Listener listener) {
        if (listener != null) {
            synchronized(listenersLock) {
                final boolean result = listeners.remove(listener);
                deliverListenersUpdate(CODE_LISTENER_REMOVED, listener);
                return result;
            }
        }
        return false;
    }

    @Override
    public final boolean containsListener(Listener listener) {
        if (listener == null) {
            return false;
        }
        synchronized(listenersLock) {
            return listeners.contains(listener);
        }
    }

    @Override
    public final boolean clearListeners() {
        try {
            synchronized(listenersLock) {
                listeners.clear();
                deliverListenersUpdate(CODE_LISTENERS_CLEARED, null);
            }
            return true;
        } catch (Throwable e) {
            e.printStackTrace();
            return false;
        }
    }

    @Override
    public final void notifyListeners(final Object event) {
        if (callThread == null) {
            doNotifyListeners(cloneListeners(), event);
        } else {
            callThread.execute(new Runnable() {
                @Override
                public void run() {
                    doNotifyListeners(cloneListeners(), event);
                }
            });
        }
    }

    @Override
    public final void visitListeners(final ListenerVisitor visitor) {
        if (visitor != null) {
            if (callThread == null || !visitSameWithNotify) {
                doVisitListeners(visitor);
            } else {
                callThread.execute(new Runnable() {
                    @Override
                    public void run() {
                        doVisitListeners(visitor);
                    }
                });
            }
        }
    }

    protected abstract void doNotifyListeners(Listener[] listeners, Object event);

    protected abstract void onListenersUpdate(int code, Listener listener);

    private final void deliverListenersUpdate(int code, Listener listener) {
        try {
            onListenersUpdate(code, listener);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private final void doVisitListeners(ListenerVisitor visitor) {
        final Listener[] listenerArray = cloneListeners();
        for (int i = 0; i < listenerArray.length; ++i) {
            visitor.visit(listenerArray[i]);
        }
    }

    private final Listener[] cloneListeners() {
        // TODO should cache the listener array with version control on concurrent environment?
        Listener[] listenerArray = new Listener[0];
        synchronized(listenersLock) {
            listenerArray = listeners.toArray(listenerArray);
        }
        return listenerArray;
    }

    // If the user forgets destroy listener executors, try to make a last effort here.
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        try {
            if (callThread != null && callThread.getMode() == ListenerExecutor.Mode.FOR_SINGLE_DISPATCHER
                    && !callThread.isDestroyed()) {
                callThread.destroy();
            }
        } catch (Throwable e) {
            // ignored
        }
    }

    public static abstract class Builder {
        private boolean distinct;
        private boolean visitSameWithNotify;
        private ListenerExecutor callThread;

        public Builder() {
            distinct = true;
            visitSameWithNotify = false;
            callThread = null;
        }

        protected Builder(AbstractDispatcher dispatcher) {
            distinct = dispatcher.distinct;
            visitSameWithNotify = dispatcher.visitSameWithNotify;
            callThread = dispatcher.callThread;
        }

        public boolean isDistinct() {
            return distinct;
        }

        public Builder setDistinct(boolean distinct) {
            this.distinct = distinct;
            return this;
        }

        public ListenerExecutor getCallThread() {
            return callThread;
        }

        public Builder setCallThread(ListenerExecutor callThread) {
            this.callThread = callThread;
            return this;
        }

        public boolean isVisitSameWithNotify() {
            return visitSameWithNotify;
        }

        public Builder setVisitSameWithNotify(boolean visitSameWithNotify) {
            this.visitSameWithNotify = visitSameWithNotify;
            return this;
        }

        public abstract AbstractDispatcher build();
    }
}
