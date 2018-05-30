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
 * A ordered listener, base on return value and {@code Consumer} container. <br/>
 * When notify event to observers, if the container is a sequence, and a {@code Consumer} object in the sequence
 * has return true(consume event), it means that may be the {@code Consumer} objects in the sequence
 * but after that one, will not be called.
 *
 * @author John Kenrinus Lee
 * @version 2016-07-15
 */
public interface Consumer<E> extends Listener<E> {
    boolean on(E event);
}
