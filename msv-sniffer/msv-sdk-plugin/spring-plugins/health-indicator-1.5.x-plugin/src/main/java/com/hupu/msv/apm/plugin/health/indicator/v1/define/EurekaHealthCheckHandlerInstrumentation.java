package com.hupu.msv.apm.plugin.health.indicator.v1.define;


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
 * @author: zhaoxudong
 * @date: 2020-01-16 14:05
 * @description:
 */
public class EurekaHealthCheckHandlerInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {
	public static final String ENHANCE_CLASS = "org.springframework.cloud.netflix.eureka.EurekaHealthCheckHandler";
	private static final String CONSTRUCTOR_INTERCEPTOR_CLASS = "com.hupu.msv.apm.plugin.health.indicator.v1.EurekaHealthCheckHandlerConstructorInterceptor";
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
					return takesArguments(1);
				}

				@Override
				public String getConstructorInterceptor() {
					return CONSTRUCTOR_INTERCEPTOR_CLASS;
				}
			}
		};
	}

	@Override
	public InstanceMethodsInterceptPoint[] getInstanceMethodsInterceptPoints() {
		return new InstanceMethodsInterceptPoint[]{
				new InstanceMethodsInterceptPoint() {
					@Override
					public ElementMatcher<MethodDescription> getMethodsMatcher() {
						return named("getStatus");
					}

					@Override
					public String getMethodsInterceptor() {
						return "com.hupu.msv.apm.plugin.health.indicator.v1.EurekaHealthCheckEnabledInterceptor";
					}

					@Override
					public boolean isOverrideArgs() {
						return false;
					}
				},
				new InstanceMethodsInterceptPoint() {
					@Override
					public ElementMatcher<MethodDescription> getMethodsMatcher() {
						return named("getHealthStatus");
					}

					@Override
					public String getMethodsInterceptor() {
						return "com.hupu.msv.apm.plugin.health.indicator.v1.EurekaHealthCheckExcludeInterceptor";
					}

					@Override
					public boolean isOverrideArgs() {
						return false;
					}
				}
		};
	}
}
