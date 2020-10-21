package com.hupu.msv.apm.plugin.ribbon.define;

import com.hupu.msv.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import com.hupu.msv.apm.agent.core.plugin.match.ClassMatch;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;


import static com.hupu.msv.apm.agent.core.plugin.match.NameMatch.byName;
import static net.bytebuddy.matcher.ElementMatchers.named;
import static net.bytebuddy.matcher.ElementMatchers.takesArguments;

/**
 * @description: 获取服务列表增强插件
 * @author: Aile
 * @create: 2019/11/06 15:44
 */
public class BaseLoadBalancerInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {


    /**
     * Enhance class.
     */
    private static final String ENHANCE_CLASS = "com.netflix.loadbalancer.BaseLoadBalancer";


    /**
     * Intercept class.
     */
    private static final String INTERCEPT_CLASS = "com.hupu.msv.apm.plugin.ribbon.BaseLoadBalancerInterceptor";


    /**
     * Intercept class.
     */
    private static final String CHOOSE_SERVER_INTERCEPT_CLASS = "com.hupu.msv.apm.plugin.ribbon.ChooseServerInterceptor";
    @Override
    protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }

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
                        return named("getAllServers")
                                .or(named("getReachableServers"));
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
                new InstanceMethodsInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named("chooseServer");
                    }

                    @Override
                    public String getMethodsInterceptor() {
                        return CHOOSE_SERVER_INTERCEPT_CLASS;
                    }

                    @Override
                    public boolean isOverrideArgs() {
                        return false;
                    }
                }
        };
    }
}
