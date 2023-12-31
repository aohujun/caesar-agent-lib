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

package com.hupu.msv.apm.plugin.jdbc.mysql;

import com.hupu.msv.apm.agent.core.context.trace.AbstractTracingSpan;
import com.hupu.msv.apm.agent.core.context.trace.SpanLayer;
import com.hupu.msv.apm.agent.core.context.trace.TraceSegment;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.test.helper.SegmentHelper;
import com.hupu.msv.apm.agent.test.tools.*;
import com.hupu.msv.apm.network.trace.component.ComponentsDefine;
import com.hupu.msv.apm.plugin.jdbc.define.StatementEnhanceInfos;
import com.hupu.msv.apm.plugin.jdbc.trace.ConnectionInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import java.lang.reflect.Method;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class StatementExecuteMethodsInterceptorTest {

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

    private StatementExecuteMethodsInterceptor serviceMethodInterceptor;

    @Mock
    private ConnectionInfo connectionInfo;
    @Mock
    private EnhancedInstance objectInstance;
    @Mock
    private Method method;
    private StatementEnhanceInfos enhanceRequireCacheObject;

    @Before
    public void setUp() {
        serviceMethodInterceptor = new StatementExecuteMethodsInterceptor();

        enhanceRequireCacheObject = new StatementEnhanceInfos(connectionInfo, "SELECT * FROM test", "CallableStatement");
        when(objectInstance.getSkyWalkingDynamicField()).thenReturn(enhanceRequireCacheObject);
        when(method.getName()).thenReturn("executeQuery");
        when(connectionInfo.getComponent()).thenReturn(ComponentsDefine.H2_JDBC_DRIVER);
        when(connectionInfo.getDBType()).thenReturn("H2");
        when(connectionInfo.getDatabaseName()).thenReturn("test");
        when(connectionInfo.getDatabasePeer()).thenReturn("localhost:3307");
    }

    @Test
    public void testCreateDatabaseSpan() throws Throwable {
        serviceMethodInterceptor.beforeMethod(objectInstance, method, new Object[] {"SELECT * FROM test"}, null, null);
        serviceMethodInterceptor.afterMethod(objectInstance, method, new Object[] {"SELECT * FROM test"}, null, null);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment segment = segmentStorage.getTraceSegments().get(0);
        assertThat(SegmentHelper.getSpans(segment).size(), is(1));
        AbstractTracingSpan span = SegmentHelper.getSpans(segment).get(0);
        SpanAssert.assertLayer(span, SpanLayer.DB);
        assertThat(span.getOperationName(), is("H2/JDBI/CallableStatement/"));
        SpanAssert.assertTag(span, 0, "sql");
        SpanAssert.assertTag(span, 1, "test");
        SpanAssert.assertTag(span, 2, "SELECT * FROM test");
    }

}
