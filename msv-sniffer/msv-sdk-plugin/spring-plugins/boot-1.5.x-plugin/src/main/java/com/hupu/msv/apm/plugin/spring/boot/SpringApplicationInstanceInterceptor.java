package com.hupu.msv.apm.plugin.spring.boot;

import com.hupu.msv.apm.agent.core.governance.gray.common.GrayRouteManager;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.util.StringUtil;

import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.Properties;

public class SpringApplicationInstanceInterceptor implements InstanceMethodsAroundInterceptor {


    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {

        if (ret == null) {
            return null;
        }

        String jarVersion = ((Class) ret).getPackage().getImplementationVersion();
        if (!StringUtil.isEmpty(jarVersion)) {
            GrayRouteManager.getInstanceMetadata().put("jar-version", jarVersion);
        }

        Properties pro = new Properties();
        InputStream in = ((Class) ret).getResourceAsStream("/git.properties");
        if (in != null) {
            pro.load(in);
            in.close();

            if (pro.containsKey("git.commit.id")) {
                GrayRouteManager.getInstanceMetadata().put("commitId", pro.getProperty("git.commit.id"));
            }

            if (pro.containsKey("git.commit.id.abbrev")) {
                GrayRouteManager.getInstanceMetadata().put("shortCommitId", pro.getProperty("git.commit.id.abbrev"));
            }
        }


        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
    }
}
