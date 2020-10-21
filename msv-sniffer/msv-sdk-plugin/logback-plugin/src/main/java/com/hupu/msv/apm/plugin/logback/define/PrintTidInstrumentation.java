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


package com.hupu.msv.apm.plugin.logback.define;

import com.hupu.msv.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import com.hupu.msv.apm.agent.core.plugin.match.ClassMatch;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static com.hupu.msv.apm.agent.core.plugin.match.NameMatch.byName;
import static net.bytebuddy.matcher.ElementMatchers.named;


public class PrintTidInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    public static final String INTERCEPT_CLASS = "com.hupu.msv.apm.plugin.logback.PrintTidIntercept";
    public static final String ENHANCE_CLASS = "ch.qos.logback.classic.PatternLayout";
    public static final String METHOD = "doLayout";

    /**
     * @return the target class, which needs active.
     */
    @Override
    protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }

    /**
     * @return null, no need to intercept constructor of enhance class.
     */
    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[]{
                new InstanceMethodsInterceptPoint() {
                    @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(METHOD);
                    }

                    @Override public String getMethodsInterceptor() {
                        return INTERCEPT_CLASS;
                    }

                    @Override public boolean isOverrideArgs() {
                        return false;
                    }
                }
        };
    }

}
