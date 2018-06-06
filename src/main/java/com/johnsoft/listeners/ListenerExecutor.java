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
 * An abstract of executor which schedule a task into a special thread.
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
public interface ListenerExecutor extends Callback<Object> {
    /** initialize the ListenerExecutor object. */
    void initialize();

    /** destroy the ListenerExecutor object. */
    void destroy();

    /** @return the status of {@code ListenerExecutor} */
    int getState();

    /** @return the mode of {@code ListenerExecutor} */
    int getMode();

    /**
     * @param mode the mode of {@code ListenerExecutor}
     * @return whether the mode setting is successful or not
     */
    boolean setMode(int mode);

    /**
     * a cancel controller, which can cancel a task scheduled.
     */
    interface Cancelable {
        void cancel();
    }

    /**
     * a executable action task
     */
    interface Executable extends Cancelable, Runnable {
    }

    /**
     * Schedule to execute a task in a special thread.
     * @see Cancelable
     * @see Executable
     */
    Cancelable execute(Executable executable);
}
