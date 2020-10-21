package com.hupu.msv.apm.plugin.foundation;

import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

/**
 * @description:
 * @author: Aile
 * @create: 2019/11/06 15:44
 */
public class FeignHystrixConcurrencyStrategyInterceptor implements InstanceMethodsAroundInterceptor {


    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult result) throws Throwable {
    }


    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {

        if (ret != null) {
            return new WrappedCallable((Callable) ret);
        }
        return null;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        ContextManager.activeSpan().errorOccurred().log(t);
    }
}
