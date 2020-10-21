package com.hupu.msv.apm.plugin.kafka.define;

import com.hupu.msv.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.match.ClassMatch;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static com.hupu.msv.apm.agent.core.plugin.match.HierarchyMatch.byHierarchyMatch;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * @author: edison.li
 * @date: 2020/7/13 6:36 下午
 * @description:
 */
public class ApiVersionsInstrumentation extends AbstractKafkaInstrumentation {

    public static final String ENHANCE_CLASS = "org.apache.kafka.clients.ApiVersions";
    public static final String ENHANCE_METHOD = "update";
    public static final String INTERCEPTOR_CLASS = "com.hupu.msv.apm.plugin.kafka.ApiVersionsInterceptor";

    @Override public ConstructorInterceptPoint[] getConstructorsInterceptPoints() {
        return new ConstructorInterceptPoint[0];
    }

    @Override public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
        return new InstanceMethodsInterceptPoint[] {
                new InstanceMethodsInterceptPoint() {
                    @Override public ElementMatcher<MethodDescription> getMethodsMatcher() {
                        return named(ENHANCE_METHOD);
                    }

                    @Override public String getMethodsInterceptor() {
                        return INTERCEPTOR_CLASS;
                    }

                    @Override public boolean isOverrideArgs() {
                        return false;
                    }
                }
        };
    }

    @Override protected ClassMatch enhanceClass() {
        return byHierarchyMatch(new String[] {ENHANCE_CLASS});
    }
}
