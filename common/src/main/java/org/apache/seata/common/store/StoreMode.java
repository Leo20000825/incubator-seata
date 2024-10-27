/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.seata.common.store;

/**
 * transaction log store mode
 *
 */
public enum StoreMode {

    /**
     * file store
     */
    FILE("file"),

    /**
     * database store
     */
    DB("db"),

    /**
     * redis store
     */
    REDIS("redis"),

    /**
     * raft store
     */
    RAFT("raft");

    private String name;

    StoreMode(String name) {
        this.name = name;
    }

    /**
     * get value of store mode
     * 
     * @param name the mode name
     * @return the store mode
     */
    public static StoreMode get(String name) {
        for (StoreMode sm : StoreMode.class.getEnumConstants()) {
            if (sm.name.equalsIgnoreCase(name)) {
                return sm;
            }
        }
        throw new IllegalArgumentException("unknown store mode:" + name);
    }

    /**
     * whether contains value of store mode
     * 
     * @param name the mode name
     * @return the boolean
     */
    public static boolean contains(String name) {
        try {
            return get(name) != null ? true : false;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    public String getName() {
        return name;
    }

}
