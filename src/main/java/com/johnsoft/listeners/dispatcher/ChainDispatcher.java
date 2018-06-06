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
import java.util.Comparator;

import com.johnsoft.listeners.Chain;
import com.johnsoft.listeners.Listener;
import com.johnsoft.listeners.ListenerExecutor;
import com.johnsoft.listeners.ListenerFoundation;

/**
 * chain dispatcher
 *
 * @author John Kenrinus Lee
 * @version 2016-07-15
 */
public class ChainDispatcher<E> extends AbstractDispatcher<E> {
    public ChainDispatcher() {
        this(new Builder<E>());
    }

    protected ChainDispatcher(Builder<E> builder) {
        super(builder);
    }

    @Override
    public Builder<E> newBuilder() {
        return new Builder<>(this);
    }

    @Override
    protected void onListenersUpdate(int code, Listener<E> listener) {
        if (code == CODE_LISTENER_ADDED) {
            if (!(listener instanceof Chain)) {
                getFoundation().throwException(new IllegalStateException("The listener added require Chain"));
            }
        } else {
            if (code != CODE_LISTENER_REMOVED && code != CODE_LISTENERS_CLEARED) {
                getFoundation().throwException(new UnsupportedOperationException("Unknown listeners update code"));
            }
        }
    }

    @Override
    protected void doNotifyListeners(Collection<Listener<E>> listeners, E event) {
        for (Listener<E> listener : listeners) {
            try {
                event = ((Chain<E>) listener).on(event);
            } catch (Throwable e) {
                getFoundation().catchThrowable(e);
            }
        }
    }

    public static class Builder<E> extends AbstractDispatcher.Builder<E> {
        public Builder() {
            super();
        }

        protected Builder(ChainDispatcher<E> dispatcher) {
            super(dispatcher);
        }

        /** {@inheritDoc} */
        @Override
        public ChainDispatcher.Builder<E> setFoundation(ListenerFoundation foundation) {
            super.setFoundation(foundation);
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public ChainDispatcher.Builder<E> setDistinctApplied(boolean distinctApplied) {
            super.setDistinctApplied(distinctApplied);
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public ChainDispatcher.Builder<E> setWeakReferenceApplied(boolean weakReferenceApplied) {
            super.setWeakReferenceApplied(weakReferenceApplied);
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public ChainDispatcher.Builder<E> setPriorityApplied(boolean priorityApplied) {
            super.setPriorityApplied(priorityApplied);
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public ChainDispatcher.Builder<E> setNotifyThread(ListenerExecutor notifyThread) {
            super.setNotifyThread(notifyThread);
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public ChainDispatcher.Builder<E> setVisitThread(ListenerExecutor visitThread) {
            super.setVisitThread(visitThread);
            return this;
        }

        /** {@inheritDoc} */
        @Override
        public ChainDispatcher.Builder<E> setPriorityComparator(Comparator<Listener<E>> priorityComparator) {
            super.setPriorityComparator(priorityComparator);
            return this;
        }

        @Override
        public ChainDispatcher<E> build() {
            return new ChainDispatcher<>(this);
        }
    }
}
