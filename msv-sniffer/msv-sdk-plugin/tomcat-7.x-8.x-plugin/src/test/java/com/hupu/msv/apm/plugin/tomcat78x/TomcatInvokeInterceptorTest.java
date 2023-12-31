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

package com.hupu.msv.apm.plugin.tomcat78x;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.hupu.msv.apm.agent.core.conf.Config;
import com.hupu.msv.apm.agent.core.context.SW3CarrierItem;
import com.hupu.msv.apm.agent.core.context.trace.AbstractTracingSpan;
import com.hupu.msv.apm.agent.core.context.trace.SpanLayer;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.test.helper.SegmentHelper;
import com.hupu.msv.apm.agent.test.helper.SpanHelper;
import com.hupu.msv.apm.agent.test.tools.SegmentStorage;
import com.hupu.msv.apm.agent.test.tools.SegmentStoragePoint;
import com.hupu.msv.apm.agent.test.tools.SpanAssert;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import com.hupu.msv.apm.agent.core.context.trace.LogDataEntity;
import com.hupu.msv.apm.agent.core.context.trace.TraceSegment;
import com.hupu.msv.apm.agent.core.context.trace.TraceSegmentRef;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.agent.test.helper.SegmentRefHelper;
import com.hupu.msv.apm.agent.test.tools.AgentServiceRule;
import com.hupu.msv.apm.agent.test.tools.TracingSegmentRunner;
import com.hupu.msv.apm.network.trace.component.ComponentsDefine;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static com.hupu.msv.apm.agent.test.tools.SpanAssert.assertComponent;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class TomcatInvokeInterceptorTest {

    private TomcatInvokeInterceptor tomcatInvokeInterceptor;
    private TomcatExceptionInterceptor tomcatExceptionInterceptor;
    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private MethodInterceptResult methodInterceptResult;

    @Mock
    private EnhancedInstance enhancedInstance;

    private Object[] arguments;
    private Class[] argumentType;

    private Object[] exceptionArguments;
    private Class[] exceptionArgumentType;

    @Before
    public void setUp() throws Exception {
        Config.Agent.ACTIVE_V1_HEADER = true;

        tomcatInvokeInterceptor = new TomcatInvokeInterceptor();
        tomcatExceptionInterceptor = new TomcatExceptionInterceptor();
        when(request.getRequestURI()).thenReturn("/test/testRequestURL");
        when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/test/testRequestURL"));
        when(response.getStatus()).thenReturn(200);
        arguments = new Object[] {request, response};
        argumentType = new Class[] {request.getClass(), response.getClass()};

        exceptionArguments = new Object[] {request, response, new RuntimeException()};
        exceptionArgumentType = new Class[] {request.getClass(), response.getClass(), new RuntimeException().getClass()};
    }

    @After
    public void clear() {
        Config.Agent.ACTIVE_V1_HEADER = false;
    }

    @Test
    public void testWithoutSerializedContextData() throws Throwable {
        tomcatInvokeInterceptor.beforeMethod(enhancedInstance, null, arguments, argumentType, methodInterceptResult);
        tomcatInvokeInterceptor.afterMethod(enhancedInstance, null, arguments, argumentType, null);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertHttpSpan(spans.get(0));
    }

    @Test
    public void testWithSerializedContextData() throws Throwable {
        when(request.getHeader(SW3CarrierItem.HEADER_NAME)).thenReturn("1.234.111|3|1|1|#192.168.1.8:18002|#/portal/|#/testEntrySpan|#AQA*#AQA*Et0We0tQNQA*");

        tomcatInvokeInterceptor.beforeMethod(enhancedInstance, null, arguments, argumentType, methodInterceptResult);
        tomcatInvokeInterceptor.afterMethod(enhancedInstance, null, arguments, argumentType, null);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);

        assertHttpSpan(spans.get(0));
        assertTraceSegmentRef(traceSegment.getRefs().get(0));
    }

    @Test
    public void testWithOccurException() throws Throwable {
        tomcatInvokeInterceptor.beforeMethod(enhancedInstance, null, arguments, argumentType, methodInterceptResult);
        tomcatInvokeInterceptor.handleMethodException(enhancedInstance, null, arguments, argumentType, new RuntimeException());
        tomcatInvokeInterceptor.afterMethod(enhancedInstance, null, arguments, argumentType, null);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);

        assertHttpSpan(spans.get(0));
        List<LogDataEntity> logDataEntities = SpanHelper.getLogs(spans.get(0));
        assertThat(logDataEntities.size(), is(1));
        SpanAssert.assertException(logDataEntities.get(0), RuntimeException.class);
    }

    @Test
    public void testWithTomcatException() throws Throwable {
        tomcatInvokeInterceptor.beforeMethod(enhancedInstance, null, arguments, argumentType, methodInterceptResult);
        tomcatExceptionInterceptor.beforeMethod(enhancedInstance, null, exceptionArguments, exceptionArgumentType, null);
        tomcatInvokeInterceptor.afterMethod(enhancedInstance, null, arguments, argumentType, null);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);

        assertHttpSpan(spans.get(0));
        List<LogDataEntity> logDataEntities = SpanHelper.getLogs(spans.get(0));
        assertThat(logDataEntities.size(), is(1));
        SpanAssert.assertException(logDataEntities.get(0), RuntimeException.class);
    }

    private void assertTraceSegmentRef(TraceSegmentRef ref) {
        assertThat(SegmentRefHelper.getEntryServiceInstanceId(ref), is(1));
        assertThat(SegmentRefHelper.getSpanId(ref), is(3));
        assertThat(SegmentRefHelper.getTraceSegmentId(ref).toString(), is("1.234.111"));
    }

    private void assertHttpSpan(AbstractTracingSpan span) {
        assertThat(span.getOperationName(), is("/test/testRequestURL"));
        assertComponent(span, ComponentsDefine.TOMCAT);
        SpanAssert.assertTag(span, 0, "http://localhost:8080/test/testRequestURL");
        assertThat(span.isEntry(), is(true));
        SpanAssert.assertLayer(span, SpanLayer.HTTP);
    }
}
