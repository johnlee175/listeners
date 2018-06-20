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
package com.johnsoft.listeners.executors;

import com.johnsoft.listeners.ListenerExecutor;

/**
 * A proxy or decorator of {@code ListenerExecutor}
 * @author John Kenrinus Lee
 * @version 2016-07-17
 */
public class ForwardingListenerExecutor implements ListenerExecutor {
    private final ListenerExecutor listenerExecutor;

    public ForwardingListenerExecutor(ListenerExecutor listenerExecutor) {
        this.listenerExecutor = listenerExecutor;
    }

    @Override
    public void initialize() {
        listenerExecutor.initialize();
    }

    @Override
    public void destroy() {
        listenerExecutor.destroy();
    }

    @Override
    public int getState() {
        return listenerExecutor.getState();
    }

    @Override
    public int getMode() {
        return listenerExecutor.getMode();
    }

    @Override
    public boolean setMode(int mode) {
        return listenerExecutor.setMode(mode);
    }

    @Override
    public Cancelable execute(Executable executable) {
        return listenerExecutor.execute(executable);
    }

    @Override
    public void on(Object event) {
        listenerExecutor.on(event);
    }
}
