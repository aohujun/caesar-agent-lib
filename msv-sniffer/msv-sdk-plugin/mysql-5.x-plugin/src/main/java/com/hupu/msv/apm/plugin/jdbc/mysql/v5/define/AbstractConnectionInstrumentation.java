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


package com.hupu.msv.apm.plugin.jdbc.mysql.v5.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import com.hupu.msv.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.match.ClassMatch;
import com.hupu.msv.apm.plugin.jdbc.define.Constants;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * {@link AbstractConnectionInstrumentation} intercepts the following methods that the class which extend
 * com.mysql.jdbc.ConnectionImpl. 
 *
 * 1. Enhance <code>prepareStatement</code> by <code>com.hupu.msv.apm.plugin.jdbc.define.JDBCPrepareStatementInterceptor</code>
 * 2. Enhance <code>prepareCall</code> by <code>com.hupu.msv.apm.plugin.jdbc.define.JDBCPrepareCallInterceptor</code>
 * 3. Enhance <code>createStatement</code> by <code>com.hupu.msv.apm.plugin.jdbc.define.JDBCStatementInterceptor</code>
 * 4. Enhance <code>commit, rollback, close, releaseSavepoint</code> by <code>com.hupu.msv.apm.plugin.jdbc.define.ConnectionServiceMethodInterceptor</code>
 *
 * @author zhangxin
 */
public abstract class AbstractConnectionInstrumentation extends AbstractMysqlInstrumentation {

    @Override public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
            new InstanceMethodsInterceptPoint() {
                @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(Constants.PREPARE_STATEMENT_METHOD_NAME);
                }

                @Override public String getMethodsInterceptor() {
                    return com.hupu.msv.apm.plugin.jdbc.mysql.Constants.CREATE_PREPARED_STATEMENT_INTERCEPTOR;
                }

                @Override public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(Constants.PREPARE_CALL_METHOD_NAME);
                }

                @Override public String getMethodsInterceptor() {
                    return com.hupu.msv.apm.plugin.jdbc.mysql.Constants.CREATE_CALLABLE_STATEMENT_INTERCEPTOR;
                }

                @Override public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(Constants.CREATE_STATEMENT_METHOD_NAME).and(takesArguments(2));
                }

                @Override public String getMethodsInterceptor() {
                    return com.hupu.msv.apm.plugin.jdbc.mysql.Constants.CREATE_STATEMENT_INTERCEPTOR;
                }

                @Override public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named(Constants.COMMIT_METHOD_NAME).or(named(Constants.ROLLBACK_METHOD_NAME)).or(named(Constants.CLOSE_METHOD_NAME)).or(named(Constants.RELEASE_SAVE_POINT_METHOD_NAME));
                }

                @Override public String getMethodsInterceptor() {
                    return Constants.SERVICE_METHOD_INTERCEPT_CLASS;
                }

                @Override public boolean isOverrideArgs() {
                    return false;
                }
            },
            new InstanceMethodsInterceptPoint() {
                @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                    return named("setCatalog");
                }

                @Override public String getMethodsInterceptor() {
                    return com.hupu.msv.apm.plugin.jdbc.mysql.Constants.SET_CATALOG_INTERCEPTOR;
                }

                @Override public boolean isOverrideArgs() {
                    return false;
                }
            }
        };

    }

    @Override protected abstract ClassMatch enhanceClass();
}
