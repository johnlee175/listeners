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

/**
 * @author John Kenrinus Lee
 * @version 2016-07-15
 */
public class ConsumerDispatcher<E> extends AbstractDispatcher {
    public ConsumerDispatcher() {
        this(new Builder<E>());
    }

    protected ConsumerDispatcher(Builder<E> builder) {
        super(builder);
    }

    @Override
    public Builder<E> newBuilder() {
        return new Builder<>(this);
    }

    @Override
    protected void onListenersUpdate(int code, Listener listener) {
        if (code == CODE_LISTENER_ADDED) {
            try { // try cast to report error at addListener
                Consumer<E> consumer = (Consumer<E>)listener;
                System.out.println("add Consumer: " + consumer);
            } catch (Throwable e) {
                throw new IllegalStateException(e);
            }
        } else {
            if (code != CODE_LISTENER_REMOVED && code != CODE_LISTENERS_CLEARED) {
                throw new UnsupportedOperationException("Unknown listeners update code");
            }
        }
    }

    @Override
    protected void doNotifyListeners(Listener[] listeners, Object event) {
        for (int i = 0; i < listeners.length; ++i) {
            try {
                if (((Consumer<E>) listeners[i]).on((E) event)) {
                    break;
                }
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
    }

    public static class Builder<E> extends AbstractDispatcher.Builder {
        public Builder() {
            super();
        }

        protected Builder(ConsumerDispatcher<E> dispatcher) {
            super(dispatcher);
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
        public ConsumerDispatcher<E> build() {
            return new ConsumerDispatcher<>(this);
        }
    }
}
