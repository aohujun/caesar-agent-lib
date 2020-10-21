package com.hupu.msv.apm.plugin.boot.loader.v1.define;


import com.hupu.msv.apm.agent.core.plugin.interceptor.ConstructorInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.InstanceMethodsInterceptPoint;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.ClassInstanceMethodsEnhancePluginDefine;
import com.hupu.msv.apm.agent.core.plugin.match.ClassMatch;
import net.bytebuddy.description.method.MethodDescription;
import net.bytebuddy.matcher.ElementMatcher;

import static com.hupu.msv.apm.agent.core.plugin.match.NameMatch.byName;
import static net.bytebuddy.matcher.ElementMatchers.named;

/**
 * @author tanghengjie
 */
public class ExecutableArchiveLauncherInstrumentation extends ClassInstanceMethodsEnhancePluginDefine {
	public static final String ENHANCE_CLASS = "org.springframework.boot.loader.ExecutableArchiveLauncher";
	public static final String ENHANCE_METHOD = "getClassPathArchives";
	private static final String INTERCEPTOR_CLASS = "com.hupu.msv.apm.plugin.boot.loader.v1.ExecutableArchiveLauncherInterceptor";

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
					return named(ENHANCE_METHOD);
				}

				@Override
				public String getMethodsInterceptor() {
					return INTERCEPTOR_CLASS;
				}

				@Override
				public boolean isOverrideArgs() {
					return false;
				}
			}
		};
	}
}
