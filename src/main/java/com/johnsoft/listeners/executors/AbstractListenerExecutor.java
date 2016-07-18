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
 * @author John Kenrinus Lee
 * @version 2016-07-17
 */
public abstract class AbstractListenerExecutor implements ListenerExecutor {
    private static final int STATE_NOT_INITIALIZED = 0;
    private static final int STATE_ALIVE = 1;
    private static final int STATE_DESTROYED = -1;

    private final Mode mode;
    private final boolean isCoverUnexectuedMode;
    protected volatile int state;

    public AbstractListenerExecutor(Mode mode, boolean isCoverUnexectuedMode) {
        this.mode = mode;
        this.isCoverUnexectuedMode = isCoverUnexectuedMode;
        this.state = STATE_NOT_INITIALIZED;
    }

    @Override
    public final boolean isNotInitialized() {
        return state == STATE_NOT_INITIALIZED;
    }

    @Override
    public final void initialize() {
        if (state != STATE_NOT_INITIALIZED) {
            throw new IllegalStateException("Can't re-initialize!");
        }
        if (doInitialize()) {
            state = STATE_ALIVE;
        }
    }

    protected abstract boolean doInitialize();

    @Override
    public final boolean isAlive() {
        return state == STATE_ALIVE;
    }

    @Override
    public final void destroy() {
        if (state == STATE_DESTROYED) {
            return;
        }
        if (state == STATE_NOT_INITIALIZED) {
            throw new IllegalStateException("Not initialize!");
        }
        try {
            doDestroy();
        } finally {
            state = STATE_DESTROYED;
        }
    }

    protected abstract void doDestroy();

    @Override
    public final boolean isDestroyed() {
        return state == STATE_DESTROYED;
    }

    @Override
    public final CancelControler execute(Runnable runnable) {
        if (state != STATE_ALIVE) {
            throw new IllegalStateException("Not alive!");
        }
        try {
            return doExecute(runnable);
        } catch (Throwable e) {
            return null;
        }
    }

    protected abstract CancelControler doExecute(Runnable runnable);

    @Override
    public final Mode getMode() {
        return mode;
    }

    @Override
    public final boolean isCoverUnexectuedMode() {
        return isCoverUnexectuedMode;
    }
}
