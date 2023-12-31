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

package com.hupu.msv.apm.plugin.grpc.v1.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import com.hupu.msv.apm.agent.core.plugin.interceptor.StaticMethodsInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.ClassStaticMethodsEnhancePluginDefine;
import com.hupu.msv.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static com.hupu.msv.apm.agent.core.plugin.bytebuddy.ArgumentTypeNameMatch.takesArgumentWithType;
import static com.hupu.msv.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * @author zhang xin
 */
public class ClientCallsInstrumentation extends ClassStaticMethodsEnhancePluginDefine {

    private static final String ENHANCE_CLASS = "io.grpc.stub.ClientCalls";
    private static final String INTERCEPTOR_CLASS = "com.hupu.msv.apm.plugin.grpc.v1.BlockingCallInterceptor";
    private static final String FUTURE_INTERCEPTOR_CLASS = "com.hupu.msv.apm.plugin.grpc.v1.AsyncUnaryRequestCallCallInterceptor";

    @Override public StaticMethodsInterceptPoint[] getStaticMethodsInterceptPoints() {
        return new StaticMethodsInterceptPoint[] {
            new StaticMethodsInterceptPoint() {
                @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("blockingUnaryCall").and(takesArgumentWithType(1, "io.grpc.MethodDescriptor"));
                }

                @Override public String getMethodsInterceptor() {
                    return INTERCEPTOR_CLASS;
                }

                @Override public boolean isOverrideArgs() {
                    return false;
                }
            },
//            new StaticMethodsInterceptPoint() {
//                @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
//                    return named("blockingServerStreamingCall").and(takesArgumentWithType(1, "io.grpc.MethodDescriptor"));
//                }
//
//                @Override public String getMethodsInterceptor() {
//                    return INTERCEPTOR_CLASS;
//                }
//
//                @Override public boolean isOverrideArgs() {
//                    return false;
//                }
//            },
            new StaticMethodsInterceptPoint() {
                @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("asyncUnaryRequestCall").and(takesArgumentWithType(2, "io.grpc.ClientCall$Listener"));
                }

                @Override public String getMethodsInterceptor() {
                    return FUTURE_INTERCEPTOR_CLASS;
                }

                @Override public boolean isOverrideArgs() {
                    return true;
                }
            }
        };
    }

    @Override protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }
}
