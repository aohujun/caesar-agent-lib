package com.hupu.msv.apm.plugin.ribbon;

import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.governance.GovernanceConfig;
import com.hupu.msv.apm.agent.core.governance.gray.common.GrayConstant;
import com.hupu.msv.apm.agent.core.governance.gray.common.GrayRouteManager;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.plugin.gray.common.strategy.route.GrayRouteServerChooser;
import com.netflix.loadbalancer.Server;

import java.lang.reflect.Method;
import java.util.List;

/**
 * @description: 获取服务列表拦截器
 * @author: Aile
 * @create: 2019/11/06 15:44
 */
public class BaseLoadBalancerInterceptor implements InstanceMethodsAroundInterceptor {

    private GrayRouteServerChooser grayRouteServerChooser = new GrayRouteServerChooser();

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult result) throws Throwable {
    }


    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        if (ret == null) {
            return null;
        }
        if (GrayRouteManager.INSTANCE.getCover()
                && ContextManager.getRuntimeContext().get(GrayConstant.EXTENSION_HTTP_HEADERS) != null) {
            // 透传header强制覆盖流量染色，且命中header
            return ret;
        }
        if (GovernanceConfig.GRAY_ENABLED) {
            return grayRouteServerChooser.choose((List<Server>) ret);
        }

        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}
