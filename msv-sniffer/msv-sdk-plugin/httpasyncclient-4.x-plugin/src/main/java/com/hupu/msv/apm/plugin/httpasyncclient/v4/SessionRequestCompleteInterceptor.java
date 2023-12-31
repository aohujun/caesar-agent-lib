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
 */

package com.hupu.msv.apm.plugin.httpasyncclient.v4;

import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.ContextSnapshot;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import org.apache.http.protocol.HttpContext;

import java.lang.reflect.Method;

/**
 * request ready(completed) so we can start our local thread span;
 *
 * @author lican
 */
public class SessionRequestCompleteInterceptor implements InstanceMethodsAroundInterceptor {

    public static ThreadLocal<HttpContext> CONTEXT_LOCAL = new ThreadLocal<HttpContext>();

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        Object[] array = (Object[]) objInst.getSkyWalkingDynamicField();
        if (array == null || array.length == 0) {
            return;
        }
        ContextSnapshot snapshot = (ContextSnapshot) array[0];
        ContextManager.createLocalSpan("httpasyncclient/local");
        if (snapshot != null) {
            ContextManager.continued(snapshot);
        }
        CONTEXT_LOCAL.set((HttpContext) array[1]);


    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {

    }
}
