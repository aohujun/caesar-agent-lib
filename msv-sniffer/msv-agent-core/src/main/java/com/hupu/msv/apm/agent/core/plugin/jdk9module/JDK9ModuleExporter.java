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

package com.hupu.msv.apm.agent.core.plugin.jdk9module;

import java.lang.instrument.Instrumentation;
import java.util.ArrayList;
import java.util.List;
import net.bytebuddy.agent.builder.AgentBuilder;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.plugin.ByteBuddyCoreClasses;

/**
 * Since JDK 9, module concept has been introduced. By supporting that, agent core needs to open the
 *
 * @author wusheng
 */
public class JDK9ModuleExporter {
    private static final ILog logger = LogManager.getLogger(JDK9ModuleExporter.class);

    private static final String[] HIGH_PRIORITY_CLASSES = {
        "com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance",
        "com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult",
        "com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.OverrideCallable",
        "com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.ConstructorInter",
        "com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstMethodsInter",
        "com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstMethodsInterWithOverrideArgs",
        "com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.StaticMethodsInter",
        "com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.StaticMethodsInterWithOverrideArgs",
    };

    /**
     * Assures that all modules of the supplied types are read by the module of any instrumented type. JDK Module system
     * was introduced since JDK9.
     *
     * The following codes work only JDK Module system exist.
     */
    public static AgentBuilder openReadEdge(Instrumentation instrumentation, AgentBuilder agentBuilder,
        EdgeClasses classes) {
        for (String className : classes.classes) {
            try {
                agentBuilder = agentBuilder.assureReadEdgeFromAndTo(instrumentation, Class.forName(className));
            } catch (ClassNotFoundException e) {
                throw new UnsupportedOperationException("Fail to open read edge for class " + className + " to public access in JDK9+", e);
            }
        }
        for (String className : HIGH_PRIORITY_CLASSES) {
            try {
                agentBuilder = agentBuilder.assureReadEdgeFromAndTo(instrumentation, Class.forName(className));
            } catch (ClassNotFoundException e) {
                throw new UnsupportedOperationException("Fail to open read edge for class " + className + " to public access in JDK9+", e);
            }
        }

        return agentBuilder;
    }

    public static class EdgeClasses {
        private List<String> classes = new ArrayList<String>();

        public EdgeClasses() {
            for (String className : ByteBuddyCoreClasses.CLASSES) {
                add(className);
            }
        }

        public void add(String className) {
            if (!classes.contains(className)) {
                classes.add(className);
            }
        }
    }
}
