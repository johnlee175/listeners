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
 * A listener manager and event dispatcher.
 *
 * @author John Kenrinus Lee
 * @version 2016-07-15
 */
public interface ListenerDispatcher<E> {
    /** add a listener to manager, if add success, return true */
    boolean addListener(Listener<E> listener);
    /** remove a listener from manager, if remove success, return true */
    boolean removeListener(Listener<E> listener);
    /** if listener is managed, return true */
    boolean containsListener(Listener<E> listener);
    /** remove all listeners from manager */
    boolean clearListeners();
    /** dispatch event to all listeners */
    void notifyListeners(E event);
    /** visit all listeners copies */
    void visitListeners(ListenerVisitor<E> visitor);
}
