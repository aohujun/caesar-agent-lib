
package com.hupu.msv.apm.plugin.hystrix.v1.define;

import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;
import com.hupu.msv.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import com.hupu.msv.apm.agent.core.plugin.match.ClassMatch;

import static net.bytebuddy.matcher.ElementMatchers.named;
import static com.hupu.msv.apm.agent.core.plugin.match.NameMatch.byName;

public class HystrixPluginsInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    public static final String INTERCEPT_CLASS = "com.hupu.msv.apm.plugin.hystrix.v1.HystrixPluginsInterceptor";
    public static final String ENHANCE_METHOD = "getCommandExecutionHook";
    public static final String GET_CONCURRENCY_STRATEGY_METHOD = "getConcurrencyStrategy";
    public static final String GET_CONCURRENCY_STRATEGY_INTERCEPT_CLASS = "com.hupu.msv.apm.plugin.hystrix.v1.HystrixConcurrencyStrategyInterceptor";
    public static final String ENHANCE_CLASS = "com.netflix.hystrix.strategy.HystrixPlugins";
    public static final String GET_PROPERTIES_STRATEGY_METHOD = "getPropertiesStrategy";
    public static final String GET_PROPERTIES_STRATEGY_INTERCEPT_CLASS = "com.hupu.msv.apm.plugin.hystrix.v1.properties.HystrixPropertiesStrategyInterceptor";
    public static final String GET_EVENT_NOTIFIER_STRATEGY_METHOD = "getEventNotifier";
    public static final String GET_EVENT_NOTIFIER_INTERCEPT_CLASS = "com.hupu.msv.apm.plugin.hystrix.v1.event.HystrixEventNotifierInterceptor";


    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[]{
                new InstanceMethodsInterceptPoint() {
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
                },
//                new InstanceMethodsInterceptPoint() {
//                    @Override
//                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
//                        return named(GET_CONCURRENCY_STRATEGY_METHOD);
//                    }
//
//                    @Override
//                    public String getMethodsInterceptor() {
//                        return GET_CONCURRENCY_STRATEGY_INTERCEPT_CLASS;
//                    }
//
//                    @Override
//                    public boolean isOverrideArgs() {
//                        return false;
//                    }
//                },
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(GET_PROPERTIES_STRATEGY_METHOD);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return GET_PROPERTIES_STRATEGY_INTERCEPT_CLASS;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                },
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(GET_EVENT_NOTIFIER_STRATEGY_METHOD);
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return GET_EVENT_NOTIFIER_INTERCEPT_CLASS;
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
        return byName(ENHANCE_CLASS);
    }
}
