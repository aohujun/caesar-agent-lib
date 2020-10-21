package com.hupu.msv.apm.plugin.spring.cloud.netflix.eurekaclient.v11;

import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;

import java.lang.reflect.Method;

/**
 * @author: liaowenqiang
 * @date: 2020-02-13
 * @description: 获取eurka服务列表
 */
public class FetchRegistryInterceptor implements InstanceMethodsAroundInterceptor {
    private static final ILog logger = LogManager.getLogger(FetchRegistryInterceptor.class);
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {

        if(ret instanceof Boolean){
            if (!(Boolean)ret){
                logger.debug("ret==false 50(millis) once again start");
                Thread.sleep(50);
                method.setAccessible(true);
               return method.invoke(objInst,allArguments);
            }
        }

        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {


    }
}
