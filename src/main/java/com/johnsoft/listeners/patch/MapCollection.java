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
package com.johnsoft.listeners.patch;

import java.util.AbstractCollection;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Wrap a Map as a Set or a Collection.
 *
 * @author John Kenrinus Lee
 * @version 2018-06-06
 */
public class MapCollection<E> extends AbstractCollection<E> implements Set<E> {
    private static final Object VALUE_CONTENT = new byte[0];

    private final Map<E, Object> map;

    public MapCollection(Map<E, Object> map) {
        if (map == null) {
           throw new IllegalArgumentException("Map<E, Object> map == null on MapCollection.<init> call");
        }
        this.map = map;
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public boolean isEmpty() {
        return map.size() == 0;
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public boolean add(E e) {
        return map.put(e, VALUE_CONTENT) == null;
    }

    @Override
    public boolean remove(Object o) {
        return map.remove(o) == VALUE_CONTENT;
    }

    @Override
    public void clear() {
        map.clear();
    }
}
