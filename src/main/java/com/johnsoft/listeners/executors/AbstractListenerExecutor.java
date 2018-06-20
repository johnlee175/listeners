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

import com.johnsoft.listeners.DefaultFoundation;
import com.johnsoft.listeners.ListenerExecutor;
import com.johnsoft.listeners.ListenerFoundation;

/**
 * {@code ListenerExecutor} base class. It provide status control and common operator.
 * The custom {@code ListenerExecutor} should refer to it.
 *
 * @author John Kenrinus Lee
 * @version 2016-07-17
 */
public abstract class AbstractListenerExecutor implements ListenerExecutor {
    public static final int STATE_NOT_INITIALIZED = 0;
    public static final int STATE_ALIVE = 1;
    public static final int STATE_DESTROYED = -1;

    public static final boolean isNotInitialized(int state) {
        return state == STATE_NOT_INITIALIZED;
    }

    public static final boolean isAlive(int state) {
        return state == STATE_ALIVE;
    }

    public static final boolean isDestroyed(int state) {
        return state == STATE_DESTROYED;
    }

    protected final ListenerFoundation foundation;

    private volatile int state;
    private int mode; // not used yet

    public AbstractListenerExecutor(ListenerFoundation foundation) {
        if (foundation == null) {
            this.foundation = new DefaultFoundation();
        } else {
            this.foundation = foundation;
        }
        this.state = STATE_NOT_INITIALIZED;
    }

    @Override
    public final void initialize() {
        if (state != STATE_NOT_INITIALIZED) {
            foundation.throwException(new IllegalStateException("Can't re-initialize!"));
            return;
        }
        if (doInitialize()) {
            state = STATE_ALIVE;
        }
    }

    @Override
    public final void destroy() {
        if (state == STATE_DESTROYED) {
            return;
        }
        if (state == STATE_NOT_INITIALIZED) {
            foundation.throwException(new IllegalStateException("Executor is not initialized!"));
            return;
        }
        try {
            doDestroy();
        } finally {
            state = STATE_DESTROYED;
        }
    }

    @Override
    public final Cancelable execute(Executable executable) {
        if (state != STATE_ALIVE) {
            foundation.throwException(new IllegalStateException("Executor is not alive!"));
        }
        return doExecute(executable);
    }

    protected abstract boolean doInitialize();

    protected abstract void doDestroy();

    protected abstract Cancelable doExecute(Executable executable);

    /** not used yet */
    @Override
    public boolean setMode(int mode) {
        this.mode = mode;
        return true;
    }

    /** not used yet */
    @Override
    public int getMode() {
        return mode;
    }

    /**
     * please use {@link #isNotInitialized(int)}, {@link #isAlive(int)}, {@link #isDestroyed(int)} instead
     */
    @Override
    public final int getState() {
        return state;
    }

    @Override
    public void on(Object event) {
        // TODO a executor can serve for one event, listener, dispatcher, or other target,
        // TODO how destroy the executor while the target finalized
    }
}
