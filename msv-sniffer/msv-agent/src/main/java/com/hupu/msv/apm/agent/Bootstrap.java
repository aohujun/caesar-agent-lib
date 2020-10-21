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

package com.hupu.msv.apm.agent;

import java.lang.instrument.Instrumentation;
import java.util.List;
import java.util.ServiceLoader;
import java.util.jar.JarFile;

import com.hupu.msv.apm.agent.core.plugin.instrumentation.Instrumenter;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.agent.builder.AgentBuilder;
import net.bytebuddy.description.NamedElement;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.dynamic.scaffold.TypeValidation;
import net.bytebuddy.matcher.ElementMatcher;
import net.bytebuddy.matcher.ElementMatchers;
import net.bytebuddy.utility.JavaModule;
import com.hupu.msv.apm.agent.core.boot.AgentPackageNotFoundException;
import com.hupu.msv.apm.agent.core.boot.ServiceManager;
import com.hupu.msv.apm.agent.core.conf.Config;
import com.hupu.msv.apm.agent.core.conf.ConfigNotFoundException;
import com.hupu.msv.apm.agent.core.conf.SnifferConfigInitializer;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.plugin.AbstractClassEnhancePluginDefine;
import com.hupu.msv.apm.agent.core.plugin.EnhanceContext;
import com.hupu.msv.apm.agent.core.plugin.InstrumentDebuggingClass;
import com.hupu.msv.apm.agent.core.plugin.PluginBootstrap;
import com.hupu.msv.apm.agent.core.plugin.PluginException;
import com.hupu.msv.apm.agent.core.plugin.PluginFinder;
import com.hupu.msv.apm.agent.core.plugin.bootstrap.BootstrapInstrumentBoost;
import com.hupu.msv.apm.agent.core.plugin.jdk9module.JDK9ModuleExporter;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toMap;
import static net.bytebuddy.matcher.ElementMatchers.*;

public class Bootstrap {
    private static final ILog logger = LogManager.getLogger(Bootstrap.class);

    /**
     * Main entrance. Use byte-buddy transform to enhance all classes, which define in plugins.
     *
     * @param agentArgs
     * @param instrumentation
     * @throws PluginException
     */
    public static void premain(String agentArgs, Instrumentation instrumentation) throws PluginException {
        final PluginFinder pluginFinder;
        AgentBuilder agentBuilder;

        try {
//            File temp = Files.createTempDirectory("bootstrap").toFile();
//            temp.deleteOnExit();
//            Class[] classes = new Class[3];
//            classes[0] = ContextTrampoline.class;
//            classes[1] = ContextStrategy.class;
//            classes[2] = Test.class;
//            Map<TypeDescription.ForLoadedType, byte[]> types = stream(classes).collect(toMap(TypeDescription.ForLoadedType::new, ClassFileLocator.ForClassLoader::read));
//
//            ClassInjector.UsingInstrumentation.of(temp, ClassInjector.UsingInstrumentation.Target.BOOTSTRAP, instrumentation)
//                    .inject(types);

            instrumentation.appendToBootstrapClassLoaderSearch(
                    new JarFile(Resources.getResourceAsTempFile("bootstrap-1.0-SNAPSHOT.jar")));

            /**
             *   开始初始化agent配置
             */
            SnifferConfigInitializer.initialize(agentArgs);

            final ByteBuddy byteBuddy = new ByteBuddy()
                    .with(TypeValidation.of(Config.Agent.IS_OPEN_DEBUGGING_CLASS));

            agentBuilder = new AgentBuilder.Default(byteBuddy).ignore(none())
                    .with(AgentBuilder.RedefinitionStrategy.RETRANSFORMATION);

            for (Instrumenter instrumenter : ServiceLoader.load(Instrumenter.class)) {
                agentBuilder = instrumenter.instrument(agentBuilder);
            }

            agentBuilder.installOn(instrumentation);

            pluginFinder = new PluginFinder(new PluginBootstrap().loadPlugins());


        } catch (ConfigNotFoundException ce) {
            logger.error(ce, "SkyWalking agent could not find config. Shutting down.");
            return;
        } catch (AgentPackageNotFoundException ape) {
            logger.error(ape, "Locate agent.jar failure. Shutting down.");
            return;
        } catch (Exception e) {
            logger.error(e, "SkyWalking agent initialized failure. Shutting down.");
            return;
        }

        agentBuilder = agentBuilder.ignore(
                nameStartsWith("net.bytebuddy.")
                        .or(nameStartsWith("org.slf4j."))
                        .or(nameStartsWith("org.groovy."))
                        .or(nameStartsWith("com.navercorp.pinpoint."))
                        // TODO: 2020-02-25 pinpoint 增强mysql8+有问题，导致启动agent报错。后面要需要解注
//                        .or(nameStartsWith("com.mysql."))
                        .or(nameStartsWith("com.hupu.rig."))
                        .or(nameContains("javassist"))
                        .or(nameContains(".asm."))
                        .or(nameStartsWith("sun.reflect"))
                        .or(named("sun.net.www.protocol.http.HttpURLConnection"))
                        .or(allSkyWalkingAgentExcludeToolkit())
                        .or(ElementMatchers.<TypeDescription>isSynthetic()));


        JDK9ModuleExporter.EdgeClasses edgeClasses = new JDK9ModuleExporter.EdgeClasses();
        try {
            agentBuilder = BootstrapInstrumentBoost.inject(pluginFinder, instrumentation, agentBuilder, edgeClasses);
        } catch (Exception e) {
            logger.error(e, "SkyWalking agent inject bootstrap instrumentation failure. Shutting down.");
            return;
        }

        try {
            agentBuilder = JDK9ModuleExporter.openReadEdge(instrumentation, agentBuilder, edgeClasses);
        } catch (Exception e) {
            logger.error(e, "SkyWalking agent open read edge in JDK 9+ failure. Shutting down.");
            return;
        }


        agentBuilder = agentBuilder
                .type(pluginFinder.buildMatch())
                .transform(new Transformer(pluginFinder))
                .with(new Listener());


        agentBuilder.installOn(instrumentation);


        try {
            ServiceManager.INSTANCE.boot();
        } catch (Exception e) {
            logger.error(e, "Skywalking agent boot failure.");
        }

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                ServiceManager.INSTANCE.shutdown();
            }
        }, "skywalking service shutdown thread"));
    }

    private static class Transformer implements AgentBuilder.Transformer {
        private PluginFinder pluginFinder;

        Transformer(PluginFinder pluginFinder) {
            this.pluginFinder = pluginFinder;
        }

        @Override
        public DynamicType.Builder<?> transform(DynamicType.Builder<?> builder, TypeDescription typeDescription,
                                                ClassLoader classLoader, JavaModule module) {
            List<AbstractClassEnhancePluginDefine> pluginDefines = pluginFinder.find(typeDescription);
            if (pluginDefines.size() > 0) {
                DynamicType.Builder<?> newBuilder = builder;
                EnhanceContext context = new EnhanceContext();
                for (AbstractClassEnhancePluginDefine define : pluginDefines) {
                    DynamicType.Builder<?> possibleNewBuilder = define.define(typeDescription, newBuilder, classLoader, context);
                    if (possibleNewBuilder != null) {
                        newBuilder = possibleNewBuilder;
                    }
                }
                if (context.isEnhanced()) {
                    logger.debug("Finish the prepare stage for {}.", typeDescription.getName());
                }

                return newBuilder;
            }

            logger.debug("Matched class {}, but ignore by finding mechanism.", typeDescription.getTypeName());
            return builder;
        }
    }

    private static ElementMatcher.Junction<NamedElement> allSkyWalkingAgentExcludeToolkit() {
        return nameStartsWith("com.hupu.msv.apm").and(not(nameStartsWith("com.hupu.msv.apm.toolkit.")));
    }

    private static class Listener implements AgentBuilder.Listener {
        @Override
        public void onDiscovery(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {

        }

        @Override
        public void onTransformation(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                                     boolean loaded, DynamicType dynamicType) {
            if (logger.isDebugEnable()) {
                logger.debug("On Transformation class {}.", typeDescription.getName());
            }

            InstrumentDebuggingClass.INSTANCE.log(dynamicType);
        }

        @Override
        public void onIgnored(TypeDescription typeDescription, ClassLoader classLoader, JavaModule module,
                              boolean loaded) {

        }

        @Override
        public void onError(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded,
                            Throwable throwable) {
            logger.error("Enhance class " + typeName + " error.", throwable);
        }

        @Override
        public void onComplete(String typeName, ClassLoader classLoader, JavaModule module, boolean loaded) {
        }
    }


}
