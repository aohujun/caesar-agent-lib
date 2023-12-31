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

package com.hupu.msv.apm.plugin.spring.mvc.v4;

import java.lang.reflect.Method;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.hupu.msv.apm.agent.core.context.trace.AbstractTracingSpan;
import com.hupu.msv.apm.agent.core.context.trace.LogDataEntity;
import com.hupu.msv.apm.agent.core.context.trace.SpanLayer;
import com.hupu.msv.apm.agent.core.context.trace.TraceSegment;
import com.hupu.msv.apm.agent.core.context.trace.TraceSegmentRef;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.agent.test.helper.SegmentHelper;
import com.hupu.msv.apm.agent.test.helper.SegmentRefHelper;
import com.hupu.msv.apm.agent.test.helper.SpanHelper;
import com.hupu.msv.apm.agent.test.tools.AgentServiceRule;
import com.hupu.msv.apm.agent.test.tools.SegmentStorage;
import com.hupu.msv.apm.agent.test.tools.SegmentStoragePoint;
import com.hupu.msv.apm.agent.test.tools.SpanAssert;
import com.hupu.msv.apm.agent.test.tools.TracingSegmentRunner;
import com.hupu.msv.apm.network.trace.component.ComponentsDefine;
import com.hupu.msv.apm.plugin.spring.mvc.commons.EnhanceRequireObjectCache;
import com.hupu.msv.apm.plugin.spring.mvc.commons.PathMappingCache;
import com.hupu.msv.apm.plugin.spring.mvc.commons.interceptor.RestMappingMethodInterceptor;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(TracingSegmentRunner.class)
public class RestMappingMethodInterceptorTest {
    private RestMappingMethodInterceptor interceptor;

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
    private NativeWebRequest nativeWebRequest;

    private Object[] arguments;
    private Class[] argumentType;

    private EnhancedInstance enhancedInstance;

    private ControllerConstructorInterceptor controllerConstructorInterceptor;

    @Before
    public void setUp() throws Exception {
        interceptor = new RestMappingMethodInterceptor();
        enhancedInstance = new RestMappingMethodInterceptorTest.MockEnhancedInstance1();
        controllerConstructorInterceptor = new ControllerConstructorInterceptor();

        when(request.getScheme()).thenReturn("http");
        when(request.getServerName()).thenReturn("localhost");
        when(request.getServerPort()).thenReturn(8080);
        when(response.getStatus()).thenReturn(200);
        when(nativeWebRequest.getNativeResponse()).thenReturn(response);

        arguments = new Object[] {request, response};
        argumentType = new Class[] {request.getClass(), response.getClass()};

    }

    @Test
    public void testGetMapping() throws Throwable {
        SpringTestCaseHelper.createCaseHandler(request, response, new SpringTestCaseHelper.CaseHandler() {
            @Override public void handleCase() throws Throwable {
                controllerConstructorInterceptor.onConstruct(enhancedInstance, null);
                RestMappingClass1 mappingClass1 = new RestMappingClass1();
                Method m = mappingClass1.getClass().getMethod("getRequestURL");
                when(request.getRequestURI()).thenReturn("/test/testRequestURL");
                when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/test/getRequestURL"));
                ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(request, response);
                RequestContextHolder.setRequestAttributes(servletRequestAttributes);

                interceptor.beforeMethod(enhancedInstance, m, arguments, argumentType, methodInterceptResult);
                interceptor.afterMethod(enhancedInstance, m, arguments, argumentType, null);
            }
        });

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);

        assertHttpSpan(spans.get(0), "/getRequestURL");
    }

    @Test
    public void testPostMapping() throws Throwable {
        SpringTestCaseHelper.createCaseHandler(request, response, new SpringTestCaseHelper.CaseHandler() {
            @Override public void handleCase() throws Throwable {
                controllerConstructorInterceptor.onConstruct(enhancedInstance, null);
                RestMappingClass1 mappingClass1 = new RestMappingClass1();
                Method m = mappingClass1.getClass().getMethod("postRequestURL");
                when(request.getRequestURI()).thenReturn("/test/testRequestURL");
                when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/test/postRequestURL"));
                ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(request, response);
                RequestContextHolder.setRequestAttributes(servletRequestAttributes);

                interceptor.beforeMethod(enhancedInstance, m, arguments, argumentType, methodInterceptResult);
                interceptor.afterMethod(enhancedInstance, m, arguments, argumentType, null);

            }
        });

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);

        assertHttpSpan(spans.get(0), "/postRequestURL");
    }

    @Test
    public void testPutMapping() throws Throwable {
        SpringTestCaseHelper.createCaseHandler(request, response, new SpringTestCaseHelper.CaseHandler() {
            @Override public void handleCase() throws Throwable {
                controllerConstructorInterceptor.onConstruct(enhancedInstance, null);
                RestMappingClass1 mappingClass1 = new RestMappingClass1();
                Method m = mappingClass1.getClass().getMethod("putRequestURL");
                when(request.getRequestURI()).thenReturn("/test/testRequestURL");
                when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/test/putRequestURL"));
                ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(request, response);
                RequestContextHolder.setRequestAttributes(servletRequestAttributes);

                interceptor.beforeMethod(enhancedInstance, m, arguments, argumentType, methodInterceptResult);
                interceptor.afterMethod(enhancedInstance, m, arguments, argumentType, null);

            }
        });

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);

        assertHttpSpan(spans.get(0), "/putRequestURL");
    }

    @Test
    public void testDeleteMapping() throws Throwable {
        SpringTestCaseHelper.createCaseHandler(request, response, new SpringTestCaseHelper.CaseHandler() {
            @Override public void handleCase() throws Throwable {
                controllerConstructorInterceptor.onConstruct(enhancedInstance, null);
                RestMappingClass1 mappingClass1 = new RestMappingClass1();
                Method m = mappingClass1.getClass().getMethod("deleteRequestURL");
                when(request.getRequestURI()).thenReturn("/test/testRequestURL");
                when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/test/deleteRequestURL"));
                ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(request, response);
                RequestContextHolder.setRequestAttributes(servletRequestAttributes);

                interceptor.beforeMethod(enhancedInstance, m, arguments, argumentType, methodInterceptResult);
                interceptor.afterMethod(enhancedInstance, m, arguments, argumentType, null);

            }
        });

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);

        assertHttpSpan(spans.get(0), "/deleteRequestURL");
    }

    @Test
    public void testPatchMapping() throws Throwable {
        SpringTestCaseHelper.createCaseHandler(request, response, new SpringTestCaseHelper.CaseHandler() {
            @Override public void handleCase() throws Throwable {
                controllerConstructorInterceptor.onConstruct(enhancedInstance, null);
                RestMappingClass1 mappingClass1 = new RestMappingClass1();
                Method m = mappingClass1.getClass().getMethod("patchRequestURL");
                when(request.getRequestURI()).thenReturn("/test/testRequestURL");
                when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/test/patchRequestURL"));
                ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(request, response);
                RequestContextHolder.setRequestAttributes(servletRequestAttributes);

                interceptor.beforeMethod(enhancedInstance, m, arguments, argumentType, methodInterceptResult);
                interceptor.afterMethod(enhancedInstance, m, arguments, argumentType, null);
            }
        });

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);

        assertHttpSpan(spans.get(0), "/patchRequestURL");
    }

    @Test
    public void testDummy() throws Throwable {
        SpringTestCaseHelper.createCaseHandler(request, response, new SpringTestCaseHelper.CaseHandler() {
            @Override public void handleCase() throws Throwable {
                controllerConstructorInterceptor.onConstruct(enhancedInstance, null);
                RestMappingClass1 mappingClass1 = new RestMappingClass1();
                Method m = mappingClass1.getClass().getMethod("dummy");
                when(request.getRequestURI()).thenReturn("/test");
                when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/test"));
                ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(request, response);
                RequestContextHolder.setRequestAttributes(servletRequestAttributes);

                interceptor.beforeMethod(enhancedInstance, m, arguments, argumentType, methodInterceptResult);
                interceptor.afterMethod(enhancedInstance, m, arguments, argumentType, null);

            }
        });

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);

        assertHttpSpan(spans.get(0), "");
    }

    @Test
    public void testWithOccurException() throws Throwable {
        SpringTestCaseHelper.createCaseHandler(request, response, new SpringTestCaseHelper.CaseHandler() {
            @Override public void handleCase() throws Throwable {
                controllerConstructorInterceptor.onConstruct(enhancedInstance, null);
                RestMappingClass1 mappingClass1 = new RestMappingClass1();
                Method m = mappingClass1.getClass().getMethod("getRequestURL");
                when(request.getRequestURI()).thenReturn("/test/testRequestURL");
                when(request.getRequestURL()).thenReturn(new StringBuffer("http://localhost:8080/test/getRequestURL"));
                ServletRequestAttributes servletRequestAttributes = new ServletRequestAttributes(request, response);
                RequestContextHolder.setRequestAttributes(servletRequestAttributes);

                interceptor.beforeMethod(enhancedInstance, m, arguments, argumentType, methodInterceptResult);
                interceptor.handleMethodException(enhancedInstance, m, arguments, argumentType, new RuntimeException());
                interceptor.afterMethod(enhancedInstance, m, arguments, argumentType, null);
            }
        });

        assertThat(segmentStorage.getTraceSegments().size(), is(1));
        TraceSegment traceSegment = segmentStorage.getTraceSegments().get(0);
        List<AbstractTracingSpan> spans = SegmentHelper.getSpans(traceSegment);

        assertHttpSpan(spans.get(0), "/getRequestURL");
        List<LogDataEntity> logDataEntities = SpanHelper.getLogs(spans.get(0));
        assertThat(logDataEntities.size(), is(1));
        SpanAssert.assertException(logDataEntities.get(0), RuntimeException.class);
    }

    private void assertTraceSegmentRef(TraceSegmentRef ref) {
        MatcherAssert.assertThat(SegmentRefHelper.getEntryServiceInstanceId(ref), is(1));
        assertThat(SegmentRefHelper.getSpanId(ref), is(3));
        MatcherAssert.assertThat(SegmentRefHelper.getTraceSegmentId(ref).toString(), is("1.444.555"));
    }

    private void assertHttpSpan(AbstractTracingSpan span, String suffix) {
        assertThat(span.getOperationName(), is("/test" + suffix));
        SpanAssert.assertComponent(span, ComponentsDefine.SPRING_MVC_ANNOTATION);
        SpanAssert.assertTag(span, 0, "http://localhost:8080/test" + suffix);
        assertThat(span.isEntry(), is(true));
        SpanAssert.assertLayer(span, SpanLayer.HTTP);
    }

    @RequestMapping(value = "/test")
    private class MockEnhancedInstance1 implements EnhancedInstance {
        private EnhanceRequireObjectCache value = new EnhanceRequireObjectCache();

        @Override
        public Object getSkyWalkingDynamicField() {
            value.setPathMappingCache(new PathMappingCache("/test"));
            return value;
        }

        @Override
        public void setSkyWalkingDynamicField(Object value) {
        }
    }

    private class RestMappingClass1 {
        @GetMapping("/getRequestURL")
        public void getRequestURL() {

        }

        @PostMapping("/postRequestURL")
        public void postRequestURL() {

        }

        @PutMapping("/putRequestURL")
        public void putRequestURL() {

        }

        @DeleteMapping("/deleteRequestURL")
        public void deleteRequestURL() {

        }

        @PatchMapping("/patchRequestURL")
        public void patchRequestURL() {

        }

        public void dummy() {

        }
    }
}
