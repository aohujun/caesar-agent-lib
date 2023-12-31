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

package com.hupu.msv.apm.plugin.shardingsphere;

import io.shardingsphere.core.executor.ShardingExecuteDataMap;
import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.ContextSnapshot;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.network.trace.component.ComponentsDefine;

import java.lang.reflect.Method;
import java.util.Map;

/**
 * {@link ExecuteInterceptor} enhances {@link io.shardingsphere.core.executor.sql.execute.SQLExecuteCallback}, creating a local span that records the execution of sql.
 *
 * @author zhangyonglun
 */
public class ExecuteInterceptor implements InstanceMethodsAroundInterceptor {
    
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) {
        ContextManager.createLocalSpan("/ShardingSphere/executeSQL/").setComponent(ComponentsDefine.SHARDING_SPHERE);
        ContextSnapshot contextSnapshot = (ContextSnapshot) ShardingExecuteDataMap.getDataMap().get(Constant.CONTEXT_SNAPSHOT);
        if (null == contextSnapshot) {
            contextSnapshot = (ContextSnapshot) ((Map) allArguments[2]).get(Constant.CONTEXT_SNAPSHOT);
        }
        if (null != contextSnapshot) {
            ContextManager.continued(contextSnapshot);
        }
    }
    
    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) {
        ContextManager.stopSpan();
        return ret;
    }
    
    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}
