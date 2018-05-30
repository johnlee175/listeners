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
 * An abstract of executor which schedule {@code Runnable} task in special thread.
 * The listener's methods will be called with ListenerExecutor.
 * We don't reuse the executor mechanism built in JDK for more precise, but if you want,
 * {@code ThreadPoolListenerExecutor} will be helpful.
 * You can also custom a executor base on {@code AbstractListenerExecutor}.
 *
 * @author John Kenrinus Lee
 * @version 2016-07-15
 *
 * @see com.johnsoft.listeners.executors.AbstractListenerExecutor
 * @see com.johnsoft.listeners.executors.ForwardingListenerExecutor
 * @see com.johnsoft.listeners.executors.TaskQueueListenerExecutor
 * @see com.johnsoft.listeners.executors.ThreadPoolListenerExecutor
 */
public interface ListenerExecutor {
    /**
     * @return True if the ListenerExecutor object is not initialized.
     *         This method should return true after initialize() be called correctly.
     */
    boolean isNotInitialized();

    /** initialize the ListenerExecutor object. */
    void initialize();

    /**
     * @return True if the ListenerExecutor object is initialized and not destroyed and available.
     *         Make sure isNotInitialized() return false and isDestroyed() return false here.
     */
    boolean isAlive();

    /** destroy the ListenerExecutor object. */
    void destroy();

    /**
     * @return True if the ListenerExecutor object is destroyed.
     *         This method should return true after destroy() be called correctly.
     */
    boolean isDestroyed();

    /**
     * Indicate that if exists a task waiting to be executed, whether or not the task will be
     * covered when the new task arrives. Use for dispatcher in the future,
     * but not yet used at any {@code ListenerExecutor} implementation class built-ins.
     */
    boolean isCoverUnexectuedMode();

    /**
     * the mode of {@code ListenerExecutor}
     */
    enum Mode {
        /**
         * Indicate the ListenerExecutor object serves a single listener.
         * @see Listener
         */
        FOR_SINGLE_LISTENER,
        /**
         * Indicate the ListenerExecutor object serves a single dispatcher, multi listeners.
         * @see ListenerDispatcher
         */
        FOR_SINGLE_DISPATCHER,
        /**
         * Indicate the ListenerExecutor object serves multi dispatchers, multi listeners.
         * Not recommended.
         */
        FOR_MULTI_SHARED
    }

    /**
     * @return the mode of {@code ListenerExecutor}
     * @see Mode
     */
    Mode getMode();

    /**
     * a cancel controller, which can cancel a task schedule.
     */
    interface CancelController {
        void cancel();
    }

    /**
     * Schedule to execute a task in a special thread.
     * @param runnable a task.
     * @return a handle for schedule, like future, use it can cancel this schedule.
     * @see CancelController
     */
    CancelController execute(Runnable runnable);
}
