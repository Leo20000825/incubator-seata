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

public enum SessionMode {
    /**
     * The File store mode.
     */
    FILE("file"),
    /**
     * The Db store mode.
     */
    DB("db"),
    /**
     * The Redis store mode.
     */
    REDIS("redis"),
    /**
     * raft store
     */
    RAFT("raft");

    private String name;

    SessionMode(String name) {
        this.name = name;
    }

    public static SessionMode get(String name) {
        for (SessionMode mode : SessionMode.values()) {
            if (mode.getName().equalsIgnoreCase(name)) {
                return mode;
            }
        }
        throw new IllegalArgumentException("unknown session mode:" + name);
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