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
     * @see com.johnsoft.listeners.ConsumerDispatcher.Builder
     */
    public static <E> ConsumerDispatcher.Builder<E> orderedDependentDispatcher(Class<E> clazz) {
        return new ConsumerDispatcher.Builder<>();
    }

    /**
     * @param clazz the generic type of CallbackDispatcher.Builder, which as the event class.
     * @return a newly CallbackDispatcher.Builder object.
     * @see com.johnsoft.listeners.CallbackDispatcher.Builder
     */
    public static <E> CallbackDispatcher.Builder<E> disorderedIndependentDispatcher(Class<E> clazz) {
        return new CallbackDispatcher.Builder<>();
    }
}
