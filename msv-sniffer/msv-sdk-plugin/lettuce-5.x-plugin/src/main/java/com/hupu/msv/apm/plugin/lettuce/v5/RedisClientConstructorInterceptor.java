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

package com.hupu.msv.apm.plugin.lettuce.v5;

import io.lettuce.core.RedisClient;
import io.lettuce.core.RedisURI;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;

public class RedisClientConstructorInterceptor implements InstanceConstructorInterceptor {

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        RedisURI redisURI = (RedisURI) allArguments[1];
        RedisClient redisClient = (RedisClient) objInst;
        EnhancedInstance optionsInst = (EnhancedInstance) redisClient.getOptions();
        optionsInst.setSkyWalkingDynamicField(redisURI.getHost() + ":" + redisURI.getPort());
    }
}
