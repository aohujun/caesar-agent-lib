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

package com.hupu.msv.apm.plugin.spring.patch.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import com.hupu.msv.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.ClassStaticMethodsEnhancePluginDefine;
import com.hupu.msv.apm.agent.core.plugin.match.ClassMatch;
import com.hupu.msv.apm.agent.core.plugin.match.NameMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * {@link AopExpressionMatchInstrumentation} indicates that exclude enhanced method in @{link EnhancedInstance} to prevent
 * side effect when use intercept all method in controller to complete their business code
 *
 * @author lican
 */
public class AopExpressionMatchInstrumentation extends ClassStaticMethodsEnhancePluginDefine {

    private  static final String ENHANCE_CLASS = "org.springframework.aop.support.MethodMatchers";
    private  static final String ENHANCE_METHOD = "matches";
    private static final String INTERCEPT_CLASS = "com.hupu.msv.apm.plugin.spring.patch.AopExpressionMatchInterceptor";

    @Override
    public final ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    public StaticMethodsInterceptPoint[] getStaticMethodsInterceptPoints() {
        return new StaticMethodsInterceptPoint[]{new StaticMethodsInterceptPoint() {
            @Override
            public ElementMatcher<MethodDescription> getMethodsMatcher() {
                return named(ENHANCE_METHOD);
            }

            @Override
            public String getMethodsInterceptor() {
                return INTERCEPT_CLASS;
            }

            @Override
            public boolean isOverrideArgs() {
                return false;
            }
        }
        };
    }

    @Override
    protected ClassMatch enhanceClass() {
        return NameMatch.byName(ENHANCE_CLASS);
    }

}
