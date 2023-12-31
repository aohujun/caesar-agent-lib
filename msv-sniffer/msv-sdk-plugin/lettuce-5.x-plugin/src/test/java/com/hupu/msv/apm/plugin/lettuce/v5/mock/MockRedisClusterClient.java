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
 *
 */

package com.hupu.msv.apm.plugin.lettuce.v5.mock;

import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;

public class MockRedisClusterClient implements EnhancedInstance {

    private Object ms;

    private EnhancedInstance options = new EnhancedInstance() {
        private Object os;

        @Override
        public Object getSkyWalkingDynamicField() {
            return os;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
            this.os = value;
        }
    };

    public EnhancedInstance getOptions() {
        return options;
    }

    public void setOptions(EnhancedInstance options) {
        this.options = options;
    }

    @Override
    public Object getSkyWalkingDynamicField() {
        return ms;
    }

    @Override
    public void setSkyWalkingDynamicField(Object value) {
        this.ms = value;
    }
}
