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


package com.hupu.msv.apm.plugin.ons.v8;

import java.util.List;
import com.hupu.msv.apm.agent.core.context.ContextSnapshot;
import com.hupu.msv.apm.agent.core.context.trace.AbstractTracingSpan;
import com.hupu.msv.apm.agent.core.context.trace.TraceSegment;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.test.helper.SegmentHelper;
import com.hupu.msv.apm.agent.test.helper.SpanHelper;
import com.hupu.msv.apm.agent.test.tools.AgentServiceRule;
import com.hupu.msv.apm.agent.test.tools.SegmentStorage;
import com.hupu.msv.apm.agent.test.tools.SegmentStoragePoint;
import com.hupu.msv.apm.agent.test.tools.SpanAssert;
import com.hupu.msv.apm.agent.test.tools.TracingSegmentRunner;
import com.hupu.msv.apm.plugin.ons.v8.define.SendCallBackEnhanceInfo;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class OnExceptionInterceptorTest {

    private OnExceptionInterceptor exceptionInterceptor;

    @SegmentStoragePoint
    private SegmentStorage segmentStorage;

    @Rule
    public AgentServiceRule serviceRule = new AgentServiceRule();

    @Mock
    private ContextSnapshot contextSnapshot;
    private SendCallBackEnhanceInfo enhanceInfo;

    @Mock
    private EnhancedInstance enhancedInstance;

    @Before
    public void setUp() {
        exceptionInterceptor = new OnExceptionInterceptor();

        enhanceInfo = new SendCallBackEnhanceInfo("test", contextSnapshot);
        when(enhancedInstance.getSkyWalkingDynamicField()).thenReturn(enhanceInfo);
    }

    @Test
    public void testOnException() throws Throwable {
        exceptionInterceptor.beforeMethod(enhancedInstance, null, new Object[] {new RuntimeException()}, null, null);
        exceptionInterceptor.afterMethod(enhancedInstance, null, new Object[] {new RuntimeException()}, null, null);

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);
        assertThat(spans.size(), is(1));

        AbstractTracingSpan exceptionSpan = spans.get(0);
        SpanAssert.assertException(SpanHelper.getLogs(exceptionSpan).get(0), RuntimeException.class);
        SpanAssert.assertOccurException(exceptionSpan, true);
    }
}
