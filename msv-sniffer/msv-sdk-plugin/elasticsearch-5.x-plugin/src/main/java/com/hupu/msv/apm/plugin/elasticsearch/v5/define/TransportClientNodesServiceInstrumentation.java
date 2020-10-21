/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hupu.msv.apm.plugin.elasticsearch.v5.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import com.hupu.msv.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import com.hupu.msv.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static com.hupu.msv.apm.agent.core.plugin.match.NameMatch.byName;

public class TransportClientNodesServiceInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    public static final String ADD_TRANSPORT_ADDRESSES_INTERCEPTOR = "com.hupu.msv.apm.plugin.elasticsearch.v5.AddTransportAddressesInterceptor";
    public static final String REMOVE_TRANSPORT_ADDRESS_INTERCEPTOR = "com.hupu.msv.apm.plugin.elasticsearch.v5.RemoveTransportAddressInterceptor";
    public static final String ENHANCE_CLASS = "org.elasticsearch.client.transport.TransportClientNodesService";

    @Override public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[]{
            new InstanceMethodsInterceptPoint() {
                @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("addTransportAddresses");
                }

                @Override public String getMethodsInterceptor() {
                    return ADD_TRANSPORT_ADDRESSES_INTERCEPTOR;
                }

                @Override public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("removeTransportAddress");
                }

                @Override public String getMethodsInterceptor() {
                    return REMOVE_TRANSPORT_ADDRESS_INTERCEPTOR;
                }

                @Override public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }

    @Override protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }
}
