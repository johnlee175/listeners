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
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.WeakHashMap;

import javax.annotation.concurrent.GuardedBy;

import com.johnsoft.listeners.DefaultFoundation;
import com.johnsoft.listeners.Listener;
import com.johnsoft.listeners.ListenerDispatcher;
import com.johnsoft.listeners.ListenerExecutor;
import com.johnsoft.listeners.ListenerFoundation;
import com.johnsoft.listeners.ListenerVisitor;
import com.johnsoft.listeners.patch.MapCollection;
import com.johnsoft.listeners.patch.WeakLinkedList;

/**
 * {@code ListenerDispatcher} base class. It provide status control and builder pattern.
 * The custom {@code ListenerDispatcher} should refer to it.
 *
 * @author John Kenrinus Lee
 * @version 2016-07-15
 */
public abstract class AbstractDispatcher<E> implements ListenerDispatcher<E> {
    private static final int DEFAULT_INITIAL_CAPACITY = 1 << 4;

    /** indicate a listener added, use for sub-class handle listeners update */
    protected static final int CODE_LISTENER_ADDED = 1;
    /** indicate all listener remove, use for sub-class handle listeners update */
    protected static final int CODE_LISTENERS_CLEARED = 0;
    /** indicate remove a listener added, use for sub-class handle listeners update */
    protected static final int CODE_LISTENER_REMOVED = -1;

    private final byte[] listenersLock = new byte[0];
    @GuardedBy("listenersLock")
    private final Collection<Listener<E>> listeners;

    // configs
    private final ListenerFoundation foundation;
    private final boolean distinctApplied;
    private final boolean weakReferenceApplied;
    private final boolean priorityApplied;
    private final ListenerExecutor notifyThread;
    private final ListenerExecutor visitThread;
    private final Comparator<Listener<E>> priorityComparator;

    protected AbstractDispatcher(Builder<E> builder) {
        foundation = builder.foundation;
        distinctApplied = builder.distinctApplied;
        weakReferenceApplied = builder.weakReferenceApplied;
        priorityApplied = builder.priorityApplied;
        notifyThread = builder.notifyThread;
        visitThread = builder.visitThread;
        priorityComparator = builder.priorityComparator;

        if (priorityApplied) {
            listeners = new PriorityQueue<>(DEFAULT_INITIAL_CAPACITY, priorityComparator);
        } else if (distinctApplied) {
            if (weakReferenceApplied) {
                listeners = new MapCollection<>(new WeakHashMap<Listener<E>, Object>(DEFAULT_INITIAL_CAPACITY));
            } else {
                listeners = new MapCollection<>(new HashMap<Listener<E>, Object>(DEFAULT_INITIAL_CAPACITY));
            }
        } else {
            if (weakReferenceApplied) {
                listeners = new WeakLinkedList<>();
            } else {
                listeners = new LinkedList<>();
            }
        }
    }

    public abstract Builder<E> newBuilder();

    /** @see ListenerFoundation */
    public final ListenerFoundation getFoundation() {
        return foundation;
    }

    /**
     * If {@code true}, add same listener to dispatcher will notify once, and notify all listeners disorderly;
     * If {@code false}, allow one listener add to dispatcher repeatedly, and notify all listeners orderly;
     */
    public final boolean isDistinctApplied() {
        return distinctApplied;
    }

    /**
     * If {@code true}, listener add in dispatcher is weak reference, so if no strong reference outside,
     * maybe listener will be GC;
     * If {@code false}, listener will be managed by strong reference;
     */
    public final boolean isWeakReferenceApplied() {
        return weakReferenceApplied;
    }

    /**
     * If {@code true}, notify events or visit listeners by listeners priority not order of addition,
     * listeners should implements {@link Comparable} or provide {@link java.util.Comparator}
     */
    public final boolean isPriorityApplied() {
        return priorityApplied;
    }

    /** Indicate which thread to notify event, if {@code null}, will notify on current calling thread */
    public final ListenerExecutor getNotifyThread() {
        return notifyThread;
    }

    /** Indicate which thread to visit listener, if {@code null}, will visit on current calling thread */
    public final ListenerExecutor getVisitThread() {
        return visitThread;
    }

    /**
     * If {@link #isPriorityApplied()} is {@code true}, and listeners is not implements {@link Comparable},
     * should provide the {@link java.util.Comparator}
     */
    public final Comparator<Listener<E>> getPriorityComparator() {
        return priorityComparator;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean addListener(Listener<E> listener) {
        if (listener != null) {
            synchronized(listenersLock) {
                final boolean result = listeners.add(listener);
                deliverListenersUpdate(CODE_LISTENER_ADDED, listener);
                return result;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean removeListener(Listener<E> listener) {
        if (listener != null) {
            synchronized(listenersLock) {
                final boolean result = listeners.remove(listener);
                deliverListenersUpdate(CODE_LISTENER_REMOVED, listener);
                return result;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean clearListeners() {
        synchronized(listenersLock) {
            listeners.clear();
            deliverListenersUpdate(CODE_LISTENERS_CLEARED, null);
        }
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final boolean containsListener(Listener<E> listener) {
        if (listener == null) {
            return false;
        }
        synchronized(listenersLock) {
            return listeners.contains(listener);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void notifyListeners(final E event) {
        if (event != null) {
            if (notifyThread == null) {
                doNotifyListeners(cloneListeners(), event);
            } else {
                notifyThread.execute(new NoncancelabilityTask(foundation) {
                    @Override
                    public void doTask() {
                        doNotifyListeners(cloneListeners(), event);
                    }
                });
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final void visitListeners(final ListenerVisitor<E> visitor) {
        if (visitor != null) {
            if (visitThread == null) {
                doVisitListeners(visitor);
            } else {
                visitThread.execute(new NoncancelabilityTask(foundation) {
                    @Override
                    public void doTask() {
                        doVisitListeners(visitor);
                    }
                });
            }
        }
    }

    protected abstract void doNotifyListeners(Collection<Listener<E>> listeners, E event);

    protected abstract void onListenersUpdate(int code, Listener<E> listener);

    private final void deliverListenersUpdate(int code, Listener<E> listener) {
        try {
            onListenersUpdate(code, listener);
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    private final void doVisitListeners(ListenerVisitor<E> visitor) {
        for(Listener<E> listener : cloneListeners()) {
            visitor.visit(listener);
        }
    }

    protected Collection<Listener<E>> cloneListeners() {
        return Collections.unmodifiableCollection(listeners);
    }

    // If the user forgets destroy listener executors, try to tip a last effort here.
    @Override
    protected void finalize() throws Throwable {
        super.finalize();
        try {
            if (notifyThread != null) {
                notifyThread.on(this + "[notifyThread]#finalize");
            }
            if (visitThread != null && visitThread != notifyThread) {
                visitThread.on(this + "[visitThread]#finalize");
            }
        } catch (Throwable e) {
            // ignored
        }
    }

    @Override
    public void on(Object event) {
    }

    public static abstract class Builder<E> {
        private ListenerFoundation foundation;
        private boolean distinctApplied;
        private boolean weakReferenceApplied;
        private boolean priorityApplied;
        private ListenerExecutor notifyThread;
        private ListenerExecutor visitThread;
        private Comparator<Listener<E>> priorityComparator;

        public Builder() {
            foundation = new DefaultFoundation();
            distinctApplied = false;
            weakReferenceApplied = false;
            priorityApplied = false;
            notifyThread = null;
            visitThread = null;
            priorityComparator = null;
        }

        protected Builder(AbstractDispatcher<E> dispatcher) {
            foundation = dispatcher.foundation;
            distinctApplied = dispatcher.distinctApplied;
            weakReferenceApplied = dispatcher.weakReferenceApplied;
            priorityApplied = dispatcher.priorityApplied;
            notifyThread = dispatcher.notifyThread;
            visitThread = dispatcher.visitThread;
            priorityComparator = dispatcher.priorityComparator;
        }

        public ListenerFoundation getFoundation() {
            return foundation;
        }

        /** @see ListenerFoundation */
        public Builder<E> setFoundation(ListenerFoundation foundation) {
            if (foundation != null) {
                this.foundation = foundation;
            }
            return this;
        }

        public boolean isDistinctApplied() {
            return distinctApplied;
        }

        /**
         * If {@code true}, add same listener to dispatcher will notify once, and notify all listeners disorderly;
         * If {@code false}, allow one listener add to dispatcher repeatedly, and notify all listeners orderly;
         */
        public Builder<E> setDistinctApplied(boolean distinctApplied) {
            this.distinctApplied = distinctApplied;
            return this;
        }

        public boolean isWeakReferenceApplied() {
            return weakReferenceApplied;
        }

        /**
         * If {@code true}, listener add in dispatcher is weak reference, so if no strong reference outside,
         * maybe listener will be GC;
         * If {@code false}, listener will be managed by strong reference;
         */
        public Builder<E> setWeakReferenceApplied(boolean weakReferenceApplied) {
            this.weakReferenceApplied = weakReferenceApplied;
            return this;
        }

        public boolean isPriorityApplied() {
            return priorityApplied;
        }

        /**
         * If {@code true}, notify events or visit listeners by listeners priority not order of addition,
         * listeners should implements {@link Comparable} or provide {@link java.util.Comparator}
         */
        public Builder<E> setPriorityApplied(boolean priorityApplied) {
            this.priorityApplied = priorityApplied;
            return this;
        }

        public ListenerExecutor getNotifyThread() {
            return notifyThread;
        }

        /** Indicate which thread to notify event, if {@code null}, will notify on current calling thread */
        public Builder<E> setNotifyThread(ListenerExecutor notifyThread) {
            this.notifyThread = notifyThread;
            return this;
        }

        public ListenerExecutor getVisitThread() {
            return visitThread;
        }

        /** Indicate which thread to visit listener, if {@code null}, will visit on current calling thread */
        public Builder<E> setVisitThread(ListenerExecutor visitThread) {
            this.visitThread = visitThread;
            return this;
        }

        public Comparator<Listener<E>> getPriorityComparator() {
            return priorityComparator;
        }

        /**
         * If {@link #isPriorityApplied()} is {@code true}, and listeners is not implements {@link Comparable},
         * should provide the {@link java.util.Comparator}
         */
        public Builder<E> setPriorityComparator(Comparator<Listener<E>> priorityComparator) {
            this.priorityComparator = priorityComparator;
            return this;
        }

        public abstract AbstractDispatcher<E> build();
    }

    public static abstract class NoncancelabilityTask implements ListenerExecutor.Executable {
        private final ListenerFoundation foundation;

        public NoncancelabilityTask(ListenerFoundation foundation) {
            this.foundation = foundation;
        }

        @Override
        public void cancel() {
            // do nothing because task can't cancel.
        }

        @Override
        public void run() {
            try {
                doTask();
            } catch (Throwable e) {
                foundation.catchThrowable(e);
            }
        }

        public abstract void doTask();
    }
}
