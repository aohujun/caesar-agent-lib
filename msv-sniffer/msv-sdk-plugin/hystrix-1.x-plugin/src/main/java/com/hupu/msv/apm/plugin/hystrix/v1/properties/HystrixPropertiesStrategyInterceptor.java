package com.hupu.msv.apm.plugin.hystrix.v1.properties;

import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.plugin.hystrix.v1.SWHystrixConcurrencyStrategyWrapper;
import com.hupu.msv.apm.plugin.hystrix.v1.SWHystrixPluginsWrapperCache;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;
import com.netflix.hystrix.strategy.executionhook.HystrixCommandExecutionHook;
import com.netflix.hystrix.strategy.metrics.HystrixMetricsPublisher;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;

import java.lang.reflect.Method;

/**
 * @author chenbaochao
 * @version 0.0.1
 * @date 2019/11/14 5:42 下午
 */
public class HystrixPropertiesStrategyInterceptor implements InstanceMethodsAroundInterceptor {
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        SWHystrixPluginsWrapperCache wrapperCache = (SWHystrixPluginsWrapperCache) objInst.getSkyWalkingDynamicField();
        if (wrapperCache == null || wrapperCache.getHystrixPropertiesStrategyWrapper() == null) {
            synchronized (objInst) {
                if (wrapperCache == null) {
                    wrapperCache = new SWHystrixPluginsWrapperCache();
                    objInst.setSkyWalkingDynamicField(wrapperCache);
                }
                if (wrapperCache.getHystrixPropertiesStrategyWrapper() == null) {

                    HystrixPropertiesStrategyWrapper wrapper = new HystrixPropertiesStrategyWrapper();
                    wrapperCache.setHystrixPropertiesStrategyWrapper(wrapper);

                    registerHystrixPropertiesStrategyWrapper(wrapper);

                    return wrapper;
                }
            }
        }
        return ret;
    }

    private void registerHystrixPropertiesStrategyWrapper(HystrixPropertiesStrategyWrapper wrapper) {
        HystrixCommandExecutionHook commandExecutionHook = HystrixPlugins
                .getInstance().getCommandExecutionHook();
        HystrixEventNotifier eventNotifier = HystrixPlugins.getInstance()
                .getEventNotifier();
        HystrixMetricsPublisher metricsPublisher = HystrixPlugins.getInstance()
                .getMetricsPublisher();
        HystrixConcurrencyStrategy concurrencyStrategy = HystrixPlugins.getInstance()
                .getConcurrencyStrategy();
        HystrixPlugins.reset();
        HystrixPlugins.getInstance().registerPropertiesStrategy(wrapper);
        HystrixPlugins.getInstance()
                .registerCommandExecutionHook(commandExecutionHook);
        HystrixPlugins.getInstance().registerEventNotifier(eventNotifier);
        HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher);
        HystrixPlugins.getInstance().registerConcurrencyStrategy(concurrencyStrategy);
    }

    @Override public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                                Class<?>[] argumentsTypes, Throwable t) {

    }
}
