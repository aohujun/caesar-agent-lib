package com.hupu.msv.apm.plugin.hystrix.v1.event;

import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.plugin.hystrix.v1.SWHystrixPluginsWrapperCache;
import com.hupu.msv.apm.plugin.hystrix.v1.properties.HystrixPropertiesStrategyWrapper;
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
 * @date 2019/11/18 5:08 上午
 */
public class HystrixEventNotifierInterceptor implements InstanceMethodsAroundInterceptor {


    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        SWHystrixPluginsWrapperCache wrapperCache = (SWHystrixPluginsWrapperCache) objInst.getSkyWalkingDynamicField();
        if (wrapperCache == null || wrapperCache.getHystrixEventNotifierWrapper() == null) {
            synchronized (objInst) {
                if (wrapperCache == null) {
                    wrapperCache = new SWHystrixPluginsWrapperCache();
                    objInst.setSkyWalkingDynamicField(wrapperCache);
                }
                if (wrapperCache.getHystrixEventNotifierWrapper() == null) {

                    HystrixEventNotifierWrapper wrapper = new HystrixEventNotifierWrapper();
                    wrapperCache.setHystrixEventNotifierWrapper(wrapper);

                    registerHystrixEventNotifierWrapper(wrapper);

                    return wrapper;
                }
            }
        }
        return ret;
    }

    private void registerHystrixEventNotifierWrapper(HystrixEventNotifierWrapper wrapper) {
        HystrixCommandExecutionHook commandExecutionHook = HystrixPlugins
                .getInstance().getCommandExecutionHook();
        HystrixPropertiesStrategy propertiesStrategy = HystrixPlugins.getInstance().getPropertiesStrategy();
        HystrixMetricsPublisher metricsPublisher = HystrixPlugins.getInstance()
                .getMetricsPublisher();
        HystrixConcurrencyStrategy concurrencyStrategy = HystrixPlugins.getInstance()
                .getConcurrencyStrategy();
        HystrixPlugins.reset();
        HystrixPlugins.getInstance().registerEventNotifier(wrapper);
        HystrixPlugins.getInstance()
                .registerCommandExecutionHook(commandExecutionHook);
        HystrixPlugins.getInstance().registerPropertiesStrategy(propertiesStrategy);
        HystrixPlugins.getInstance().registerMetricsPublisher(metricsPublisher);
        HystrixPlugins.getInstance().registerConcurrencyStrategy(concurrencyStrategy);
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {

    }
}
