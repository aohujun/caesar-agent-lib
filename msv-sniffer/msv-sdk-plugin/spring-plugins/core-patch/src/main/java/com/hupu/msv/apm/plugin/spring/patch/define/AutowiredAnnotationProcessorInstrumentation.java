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


package com.hupu.msv.apm.plugin.spring.patch.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import com.hupu.msv.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import com.hupu.msv.apm.agent.core.plugin.match.ClassMatch;
import com.hupu.msv.apm.agent.core.plugin.match.NameMatch;

import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * {@link AutowiredAnnotationProcessorInstrumentation} indicates a spring core class patch for making sure the
 * determineCandidateConstructors method in the class {@link org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor}
 * works in spring designed ways
 *
 * @author zhang xin
 */
public class AutowiredAnnotationProcessorInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {
    private static final String ENHANCE_CLASS = "org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor";
    private static final String ENHANCE_METHOD = "determineCandidateConstructors";
    private static final String INTERCEPTOR_CLASS = "com.hupu.msv.apm.plugin.spring.patch.AutowiredAnnotationProcessorInterceptor";

    @Override public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[] {
            new ConstructorInterceptPoint() {
                @Override public ElementMatcher<MethodDescription> getConstructorMatcher() {
                    return any();
                }

                @Override public String getConstructorInterceptor() {
                    return INTERCEPTOR_CLASS;
                }
            }
        };
    }

    @Override public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
            new InstanceMethodsInterceptPoint() {
                @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(ENHANCE_METHOD);
                }

                @Override public String getMethodsInterceptor() {
                    return INTERCEPTOR_CLASS;
                }

                @Override public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }

    @Override protected ClassMatch enhanceClass() {
        return NameMatch.byName(ENHANCE_CLASS);
    }
}
