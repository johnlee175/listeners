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
package com.johnsoft.listeners.test;

import com.johnsoft.listeners.Callback;

/**
 * @author John Kenrinus Lee
 * @version 2016-07-18
 */
public class TestTemp {
    public static void main(String[] args) {
        MyResource resource = new MyResource();
        Callback<MyEvent> callback1 = new Callback<MyEvent>() {
            @Override
            public void on(MyEvent event) {
                System.out.println(Thread.currentThread() + ", " + event);
            }
        };
        Callback<MyEvent> callback2 = new Callback<MyEvent>() {
            @Override
            public void on(MyEvent event) {
                System.out.println(Thread.currentThread() + ", " + event);
            }
        };
        resource.addListener(callback1);
        resource.addListener(callback2);
        resource.onDataChanged(new MyEvent("Hello World"));
        resource.onDataChanged(new MyEvent("Welcome"));
        resource.removeListener(callback1);
        resource.removeListener(callback2);
    }
}
