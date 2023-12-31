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


package com.hupu.msv.apm.plugin.jedis.v2;

import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import com.hupu.msv.apm.agent.core.context.util.PeerFormat;
import redis.clients.jedis.HostAndPort;

import java.util.Set;

public class JedisClusterConstructorWithListHostAndPortArgInterceptor implements InstanceConstructorInterceptor {

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        StringBuilder redisConnInfo = new StringBuilder();
        Set<HostAndPort> hostAndPorts = (Set<HostAndPort>)allArguments[0];
        for (HostAndPort hostAndPort : hostAndPorts) {
            redisConnInfo.append(hostAndPort.toString()).append(";");
        }

        objInst.setSkyWalkingDynamicField(PeerFormat.shorten(redisConnInfo.toString()));
    }
}
