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


package com.hupu.msv.apm.plugin.redisson.v3.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import com.hupu.msv.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import com.hupu.msv.apm.agent.core.plugin.match.ClassMatch;

import static com.hupu.msv.apm.agent.core.plugin.bytebuddy.ArgumentTypeNameMatch.takesArgumentWithType;
import static net.bytebuddy.matcher.ElementMatchers.any;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static com.hupu.msv.apm.agent.core.plugin.match.NameMatch.byName;

/**
 * @author zhaoyuguang
 */
public class ConnectionManagerInstrumentation extends AbstractRedissonInstrumentation {

//    private static final String ARGUMENT_TYPE_NAME = "org.redisson.config.Config";

    private static final String ENHANCE_CLASS = "org.redisson.connection.MasterSlaveConnectionManager";

    private static final String CONNECTION_MANAGER_INTERCEPTOR = "com.hupu.msv.apm.plugin.redisson.v3.ConnectionManagerInterceptor";

//    private static final String CONNECTION_MANAGER_CONSTRUCTOR_INTERCEPTOR = "com.hupu.msv.apm.plugin.redisson.v3.ConnectionManagerConstructorInterceptor";

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
//        return new ConstructorInterceptPoint[] {
//                new ConstructorInterceptPoint() {
//                    @Override
//                    public ElementMatcher<MethodDescription> getConstructorMatcher() {
//                        return takesArgumentWithType(0, ARGUMENT_TYPE_NAME);
//                    }
//
//                    @Override
//                    public String getConstructorInterceptor() {
//                        return CONNECTION_MANAGER_CONSTRUCTOR_INTERCEPTOR;
//                    }
//                }
//        };
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[]{
            new InstanceMethodsInterceptPoint() {
                @Override
                public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("createClient");
                }

                @Override
                public String getMethodsInterceptor() {
                    return CONNECTION_MANAGER_INTERCEPTOR;
                }

                @Override
                public boolean isOverrideArgs() {
                    return false;
                }
            }
        };
    }

    @Override
    public ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }
}
