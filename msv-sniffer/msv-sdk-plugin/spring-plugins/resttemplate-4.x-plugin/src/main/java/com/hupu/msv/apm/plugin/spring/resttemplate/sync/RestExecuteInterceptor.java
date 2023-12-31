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

package com.hupu.msv.apm.plugin.spring.resttemplate.sync;

import java.lang.reflect.Method;
import java.net.URI;
import com.hupu.msv.apm.agent.core.boot.ServiceManager;
import com.hupu.msv.apm.agent.core.conf.Config;
import com.hupu.msv.apm.agent.core.context.ContextCarrier;
import com.hupu.msv.apm.agent.core.context.OperationNameFormatService;
import com.hupu.msv.apm.agent.core.context.tag.Tags;
import com.hupu.msv.apm.agent.core.context.trace.SpanLayer;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.trace.AbstractSpan;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.network.trace.component.ComponentsDefine;
import org.springframework.http.HttpMethod;

public class RestExecuteInterceptor implements InstanceMethodsAroundInterceptor {
    private OperationNameFormatService nameFormatService;

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        MethodInterceptResult result) throws Throwable {
        final URI requestURL = (URI)allArguments[0];
        final HttpMethod httpMethod = (HttpMethod)allArguments[1];
        final ContextCarrier contextCarrier = new ContextCarrier();

        if (nameFormatService == null) {
            nameFormatService = ServiceManager.INSTANCE.findService(OperationNameFormatService.class);
        }

        String remotePeer = requestURL.getHost() + ":" + (requestURL.getPort() > 0 ? requestURL.getPort() : "https".equalsIgnoreCase(requestURL.getScheme()) ? 443 : 80);
        String formatURIPath = nameFormatService.formatOperationName(Config.Plugin.OPGroup.RestTemplate.class, requestURL.getPath());
        AbstractSpan span = ContextManager.createExitSpan(formatURIPath, contextCarrier, remotePeer);

        span.setComponent(ComponentsDefine.SPRING_REST_TEMPLATE);
        Tags.URL.set(span, requestURL.getScheme() + "://" + requestURL.getHost() + ":" + requestURL.getPort() + requestURL.getPath());
        Tags.HTTP.METHOD.set(span, httpMethod.toString());
        SpanLayer.asHttp(span);

        objInst.setSkyWalkingDynamicField(contextCarrier);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}
