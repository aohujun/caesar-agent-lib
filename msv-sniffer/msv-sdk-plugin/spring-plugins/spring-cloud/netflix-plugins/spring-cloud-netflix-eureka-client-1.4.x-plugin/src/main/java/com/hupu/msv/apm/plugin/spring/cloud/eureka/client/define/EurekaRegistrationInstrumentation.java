package com.hupu.msv.apm.plugin.spring.cloud.eureka.client.define;

import com.hupu.msv.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import com.hupu.msv.apm.agent.core.plugin.match.ClassMatch;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static com.hupu.msv.apm.agent.core.plugin.bytebuddy.ArgumentTypeNameMatch.takesArgumentWithType;
import static com.hupu.msv.apm.agent.core.plugin.match.NameMatch.byName;

public class EurekaRegistrationInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {

    /**
     * Enhance class.
     */
    private static final String ENHANCE_CLASS = "org.springframework.cloud.netflix.eureka.serviceregistry.EurekaRegistration";

    /**
     * Intercept class.
     */
    private static final String INTERCEPT_CLASS = "com.hupu.msv.apm.plugin.spring.cloud.eureka.client.EurekaRegistrationInterceptor";


    private static final String CONSTRUCTOR_INTERCEPT_TYPE = "org.springframework.cloud.netflix.eureka.CloudEurekaInstanceConfig";


    @Override
    protected ClassMatch enhanceClass() {
        return byName(ENHANCE_CLASS);
    }

    @Override
    public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[]{
                new ConstructorInterceptPoint() {
                    @Override
                    public ElementMatcher<MethodDescription> getConstructorMatcher() {
                        return takesArgumentWithType(0, CONSTRUCTOR_INTERCEPT_TYPE);
                    }

                    @Override
                    public String getConstructorInterceptor() {
                        return INTERCEPT_CLASS;
                    }
                }

        };
    }

    @Override
    public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[0];
    }
}
