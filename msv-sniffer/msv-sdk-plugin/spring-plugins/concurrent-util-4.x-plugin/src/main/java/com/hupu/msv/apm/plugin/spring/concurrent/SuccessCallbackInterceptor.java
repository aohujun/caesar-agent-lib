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

package com.hupu.msv.apm.plugin.spring.concurrent;

import java.lang.reflect.Method;
import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.ContextSnapshot;
import com.hupu.msv.apm.agent.core.context.trace.AbstractSpan;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.agent.core.util.CaesarAgentUtil;
import com.hupu.msv.apm.plugin.spring.commons.EnhanceCacheObjects;

public class SuccessCallbackInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        MethodInterceptResult result) throws Throwable {
        EnhanceCacheObjects cacheValues = (EnhanceCacheObjects)objInst.getSkyWalkingDynamicField();
        if (cacheValues == null) {
            return;
        }
        ContextSnapshot snapshot = cacheValues.getContextSnapshot();
        String traceId = null;
        if (snapshot != null && snapshot.isValid() && snapshot.getDistributedTraceId() != null) {
            traceId = snapshot.getDistributedTraceId().toString();
        }
        AbstractSpan span = ContextManager.createLocalSpan("future/successCallback:" + cacheValues.getOperationName(), CaesarAgentUtil.isSampled(traceId));
        span.setComponent(cacheValues.getComponent()).setLayer(cacheValues.getSpanLayer());
        ContextManager.continued(snapshot);
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        Object ret) throws Throwable {
        EnhanceCacheObjects cacheValues = (EnhanceCacheObjects)objInst.getSkyWalkingDynamicField();
        if (cacheValues == null) {
            return ret;
        }
        ContextManager.stopSpan();
        return ret;
    }

    @Override public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
        Class<?>[] argumentsTypes, Throwable t) {
        EnhanceCacheObjects cacheValues = (EnhanceCacheObjects)objInst.getSkyWalkingDynamicField();
        if (cacheValues == null) {
            return;
        }
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}
