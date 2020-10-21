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

package com.hupu.msv.apm.plugin.hystrix.v1;

import java.lang.reflect.Method;

import com.google.gson.GsonBuilder;
import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.ContextSnapshot;
import com.hupu.msv.apm.agent.core.context.trace.AbstractSpan;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.agent.core.util.CaesarAgentUtil;
import com.hupu.msv.apm.network.governance.CircuitBreakerConfig;
import com.hupu.msv.apm.network.governance.Result;
import com.hupu.msv.apm.network.trace.component.ComponentsDefine;
import com.hupu.msv.apm.plugin.hystrix.v1.factory.MatchedRelationRuleFactory;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandKey;
import org.springframework.util.StringUtils;

public class HystrixCommandGetFallbackInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        EnhanceRequireObjectCache enhanceRequireObjectCache = (EnhanceRequireObjectCache) objInst.getSkyWalkingDynamicField();
        ContextSnapshot snapshot = enhanceRequireObjectCache.getContextSnapshot();
        AbstractSpan activeSpan;
        if (snapshot != null && snapshot.isValid() && snapshot.getDistributedTraceId() != null) {
            String traceId = snapshot.getDistributedTraceId().toString();
            activeSpan = ContextManager.createLocalSpan(enhanceRequireObjectCache.getOperationNamePrefix() + "/Fallback", CaesarAgentUtil.isSampled(traceId));
        } else {
            activeSpan = ContextManager.createLocalSpan(enhanceRequireObjectCache.getOperationNamePrefix() + "/Fallback");
        }
        activeSpan.setComponent(ComponentsDefine.HYSTRIX);
        if (snapshot != null) {
            ContextManager.continued(snapshot);
        }

        if (objInst instanceof HystrixCommand) {
            HystrixCommand command = (HystrixCommand) objInst;
            HystrixCommandKey commandKey = command.getCommandKey();
//            //feign调用 生成的是内部类 特殊处理
//            if (command.getClass().getName().startsWith("feign.hystrix.HystrixInvocationHandler")) {
//                setFallbackResult(commandKey.name(), result);
//            } else {
//                Method fallbackUserDefinedMethod = HystrixCommand.class.getDeclaredMethod("isFallbackUserDefined");
//                fallbackUserDefinedMethod.setAccessible(true);
//                boolean isUserDefined = (boolean) fallbackUserDefinedMethod.invoke(command);
//                if (!isUserDefined) {
//                    setFallbackResult(commandKey.name(), result);
//                }
//            }
            setFallbackResult(commandKey.name(), result);
        }
    }

    private void setFallbackResult(String commandKey, MethodInterceptResult result) {
        CircuitBreakerConfig circuitBreakerConfig = MatchedRelationRuleFactory.getInstance().getCircuitBreakerConfig(commandKey);
        if (circuitBreakerConfig == null || !Boolean.parseBoolean(circuitBreakerConfig.getResultEnabled())) {
            return;
        }
        Result fallbackResult = circuitBreakerConfig.getResult();
        if (fallbackResult != null && !StringUtils.isEmpty(fallbackResult.getResponse())) {
            if ("void".equals(MatchedRelationRuleFactory.getReturnMap().get(commandKey).getName())) {
                return;
            }
            result.defineReturnValue(new GsonBuilder().create().fromJson(fallbackResult.getResponse(), MatchedRelationRuleFactory.getReturnMap().get(commandKey)));
        }
    }


    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        ContextManager.stopSpan();

        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}
