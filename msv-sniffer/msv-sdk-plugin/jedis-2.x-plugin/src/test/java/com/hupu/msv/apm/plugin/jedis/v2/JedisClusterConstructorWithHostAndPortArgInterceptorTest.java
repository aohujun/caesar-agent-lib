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

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import redis.clients.jedis.HostAndPort;

import static org.mockito.Mockito.verify;

@RunWith(MockitoJUnitRunner.class)
public class JedisClusterConstructorWithHostAndPortArgInterceptorTest {

    private JedisClusterConstructorWithHostAndPortArgInterceptor interceptor;

    @Mock
    private EnhancedInstance enhancedInstance;

    @Before
    public void setUp() throws Exception {
        interceptor = new JedisClusterConstructorWithHostAndPortArgInterceptor();
    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void onConstruct() throws Exception {
        interceptor.onConstruct(enhancedInstance, new Object[] {new HostAndPort("127.0.0.1", 6379)});
        verify(enhancedInstance).setSkyWalkingDynamicField("127.0.0.1:6379");
    }

}
