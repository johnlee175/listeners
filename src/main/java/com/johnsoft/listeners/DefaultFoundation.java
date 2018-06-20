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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * The default implementation of {@code ListenerFoundation}
 *
 * @author John Kenrinus Lee
 * @version 2016-07-15
 */
public class DefaultFoundation implements ListenerFoundation {
    private volatile boolean debug;

    public void setDebug(boolean debug) {
        this.debug = debug;
    }

    @Override
    public void log(String message) {
        if (debug) {
            System.out.println(message);
        } else {
            System.err.println(message);
        }
    }

    @Override
    public void throwException(RuntimeException exception) {
        throw exception;
    }

    @Override
    public void catchThrowable(Throwable throwable) {
        if (throwable != null) {
            throwable.printStackTrace();
        }
    }

    @Override
    public Map<String, Object> getContext() {
        return Collections.synchronizedMap(new HashMap<String, Object>(16));
    }
}
