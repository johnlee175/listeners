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

import com.johnsoft.listeners.dispatcher.CallbackDispatcher;
import com.johnsoft.listeners.dispatcher.ChainDispatcher;
import com.johnsoft.listeners.dispatcher.ConsumerDispatcher;

/**
 * A utilities and factory of Builder which implements ListenerDispatcher.
 *
 * @author John Kenrinus Lee
 * @version 2016-07-15
 *
 * @see ListenerDispatcher
 */
public final class Dispatchers {
    private Dispatchers() {}

    /**
     * @param clazz the generic type of ConsumerDispatcher.Builder, which as the event class.
     * @return a newly ConsumerDispatcher.Builder object.
     * @see ConsumerDispatcher.Builder
     */
    public static <E> ConsumerDispatcher.Builder<E> consumerDispatcher(Class<E> clazz) {
        return new ConsumerDispatcher.Builder<>();
    }

    /**
     * @param clazz the generic type of ChainDispatcher.Builder, which as the event class.
     * @return a newly ChainDispatcher.Builder object.
     * @see ChainDispatcher.Builder
     */
    public static <E> ChainDispatcher.Builder<E> chainDispatcher(Class<E> clazz) {
        return new ChainDispatcher.Builder<>();
    }

    /**
     * @param clazz the generic type of CallbackDispatcher.Builder, which as the event class.
     * @return a newly CallbackDispatcher.Builder object.
     * @see CallbackDispatcher.Builder
     */
    public static <E> CallbackDispatcher.Builder<E> callbackDispatcher(Class<E> clazz) {
        return new CallbackDispatcher.Builder<>();
    }
}
