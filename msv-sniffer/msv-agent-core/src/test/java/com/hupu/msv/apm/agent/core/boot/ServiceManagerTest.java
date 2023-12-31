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


package com.hupu.msv.apm.agent.core.boot;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.IgnoredTracerContext;
import com.hupu.msv.apm.agent.core.context.TracingContext;
import com.hupu.msv.apm.agent.core.context.TracingContextListener;
import com.hupu.msv.apm.agent.core.remote.GRPCChannelListener;
import com.hupu.msv.apm.agent.core.remote.GRPCChannelManager;
import com.hupu.msv.apm.agent.core.remote.TraceSegmentServiceClient;
import com.hupu.msv.apm.agent.core.sampling.SamplingService;
import com.hupu.msv.apm.agent.core.test.tools.AgentServiceRule;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class ServiceManagerTest {

    @Rule
    public AgentServiceRule agentServiceRule = new AgentServiceRule();

    @AfterClass
    public static void afterClass() {
        ServiceManager.INSTANCE.shutdown();
    }

    @Test
    public void testServiceDependencies() throws Exception {
        HashMap<Class, BootService> registryService = getFieldValue(ServiceManager.INSTANCE, "bootedServices");

        assertThat(registryService.size(), is(10));

        assertTraceSegmentServiceClient(ServiceManager.INSTANCE.findService(TraceSegmentServiceClient.class));
        assertContextManager(ServiceManager.INSTANCE.findService(ContextManager.class));
        assertGRPCChannelManager(ServiceManager.INSTANCE.findService(GRPCChannelManager.class));
        assertSamplingService(ServiceManager.INSTANCE.findService(SamplingService.class));

        assertTracingContextListener();
        assertIgnoreTracingContextListener();
    }

    private void assertIgnoreTracingContextListener() throws Exception {
        List<TracingContextListener> listeners = getFieldValue(IgnoredTracerContext.ListenerManager.class, "LISTENERS");
        assertThat(listeners.size(), is(0));
    }

    private void assertTracingContextListener() throws Exception {
        List<TracingContextListener> listeners = getFieldValue(TracingContext.ListenerManager.class, "LISTENERS");
        assertThat(listeners.size(), is(1));

        assertThat(listeners.contains(ServiceManager.INSTANCE.findService(TraceSegmentServiceClient.class)), is(true));
    }


    private void assertGRPCChannelManager(GRPCChannelManager service) throws Exception {
        assertNotNull(service);

        List<GRPCChannelListener> listeners = getFieldValue(service, "listeners");
        assertEquals(listeners.size(), 3);
    }

    private void assertSamplingService(SamplingService service) {
        assertNotNull(service);
    }

    private void assertContextManager(ContextManager service) {
        assertNotNull(service);
    }

    private void assertTraceSegmentServiceClient(TraceSegmentServiceClient service) {
        assertNotNull(service);
    }

    private <T> T getFieldValue(Object instance, String fieldName) throws Exception {
        Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T)field.get(instance);
    }

    private <T> T getFieldValue(Class clazz, String fieldName) throws Exception {
        Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T)field.get(clazz);
    }

}
