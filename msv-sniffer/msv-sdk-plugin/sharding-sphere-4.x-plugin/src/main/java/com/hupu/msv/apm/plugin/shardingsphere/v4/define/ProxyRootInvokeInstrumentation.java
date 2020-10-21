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

package com.hupu.msv.apm.plugin.shardingsphere.v4.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import com.hupu.msv.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import com.hupu.msv.apm.agent.core.plugin.match.ClassMatch;
import com.hupu.msv.apm.agent.core.plugin.match.NameMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * ProxyRootInvokeInstrumentation presents that skywalking intercepts org.apache.shardingsphere.shardingproxy.frontend.command.CommandExecutorTask.
 *
 * @author zhangyonglun
 */
public class ProxyRootInvokeInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {
    
    private static final String ENHANCE_CLASS = "org.apache.shardingsphere.shardingproxy.frontend.command.CommandExecutorTask";
    
    private static final String PROXY_ROOT_INVOKE_INTERCEPTOR_CLASS = "com.hupu.msv.apm.plugin.shardingsphere.v4.ProxyRootInvokeInterceptor";
    
    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[]{
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("run");
                }
                
                @Override
                public String getMethodsInterceptor() {
                    return PROXY_ROOT_INVOKE_INTERCEPTOR_CLASS;
                }
                
                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }
    
    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }
    
    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName(ENHANCE_CLASS);
    }
}
