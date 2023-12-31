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

package com.hupu.msv.apm.plugin.grpc.v1;

import io.grpc.Channel;
import io.grpc.MethodDescriptor;
import java.lang.reflect.Method;
import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.trace.AbstractSpan;
import com.hupu.msv.apm.agent.core.context.trace.SpanLayer;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.StaticMethodsAroundInterceptor;
import com.hupu.msv.apm.network.trace.component.ComponentsDefine;

import static com.hupu.msv.apm.plugin.grpc.v1.OperationNameFormatUtil.formatOperationName;

/**
 * @author zhang xin
 */
public class BlockingCallInterceptor implements StaticMethodsAroundInterceptor {

    @Override public void beforeMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes,
        MethodInterceptResult result) {
        Channel channel = (Channel)allArguments[0];
        MethodDescriptor methodDescriptor = (MethodDescriptor)allArguments[1];
        final AbstractSpan span = ContextManager.createExitSpan(formatOperationName(methodDescriptor), channel.authority());
        span.setComponent(ComponentsDefine.GRPC);
        SpanLayer.asRPCFramework(span);
    }

    @Override public Object afterMethod(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes,
        Object ret) {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(Class clazz, Method method, Object[] allArguments, Class<?>[] parameterTypes,
        Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}
