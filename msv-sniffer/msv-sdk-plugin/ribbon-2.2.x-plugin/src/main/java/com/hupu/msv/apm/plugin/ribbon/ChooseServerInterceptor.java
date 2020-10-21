package com.hupu.msv.apm.plugin.ribbon;

import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.governance.GovernanceConfig;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.netflix.loadbalancer.BaseLoadBalancer;

import java.lang.reflect.Method;

import static com.hupu.msv.apm.plugin.ribbon.loadbalancer.LoadBalancerRuleHandler.resetRule;

/**
 * @author: zhaoxudong
 * @date: 2020-01-14 11:57
 * @description:
 */
public class ChooseServerInterceptor implements InstanceMethodsAroundInterceptor {
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {
        // 选择服务之前更换负载均衡策略
        if (GovernanceConfig.LOAD_BALANCER_ENABLED && BaseLoadBalancer.class.isAssignableFrom(objInst.getClass())) {
            BaseLoadBalancer loadBalancer = (BaseLoadBalancer) objInst;
            resetRule(loadBalancer);
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        // TODO: 2020-02-13 选择服务之后，上报数据。上报的信息：负载均衡策略是什么、选择的服务实例是哪个等
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}
