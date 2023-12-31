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

package com.hupu.msv.apm.plugin.jdbc;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import com.hupu.msv.apm.agent.core.conf.Config;
import com.hupu.msv.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import com.hupu.msv.apm.plugin.jdbc.define.Constants;

import java.util.Set;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.none;
import static com.hupu.msv.apm.plugin.jdbc.define.Constants.PS_IGNORABLE_SETTERS;
import static com.hupu.msv.apm.plugin.jdbc.define.Constants.PS_SETTERS;

/**
 * @author kezhenxu94
 */
public class PSSetterDefinitionOfJDBCInstrumentation implements InstanceMethodsInterceptPoint {
    private final boolean ignorable;

    public PSSetterDefinitionOfJDBCInstrumentation(boolean ignorable) {
        this.ignorable = ignorable;
    }

    @Override
    public ElementMatcher<MethodDescription> getMethodsMatcher() {
        ElementMatcher.Junction<MethodDescription> matcher = none();

        if (Config.Plugin.MySQL.TRACE_SQL_PARAMETERS) {
            final Set<String> setters = ignorable ? PS_IGNORABLE_SETTERS : PS_SETTERS;
            for (String setter : setters) {
                matcher = matcher.or(named(setter));
            }
        }

        return matcher;
    }

    @Override
    public String getMethodsInterceptor() {
        return ignorable
            ? Constants.PREPARED_STATEMENT_IGNORABLE_SETTER_METHODS_INTERCEPTOR
            : Constants.PREPARED_STATEMENT_SETTER_METHODS_INTERCEPTOR;
    }

    @Override
    public boolean isOverrideArgs() {
        return false;
    }
}
