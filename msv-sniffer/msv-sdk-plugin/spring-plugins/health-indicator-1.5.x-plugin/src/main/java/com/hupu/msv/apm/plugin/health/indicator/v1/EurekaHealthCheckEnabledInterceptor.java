package com.hupu.msv.apm.plugin.health.indicator.v1;

import com.hupu.msv.apm.agent.core.governance.GovernanceConfig;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.netflix.appinfo.InstanceInfo;

import java.lang.reflect.Method;

/**
 * @author: zhaoxudong
 * @date: 2020-02-13 17:38
 * @description: eureka client health check是否开启
 * eureka.client.eurekahealthcheck.enabled=true 这个开关是eureka自己的开关，只有为true时才会进入这个类的方法。
 *
 */
public class EurekaHealthCheckEnabledInterceptor implements InstanceMethodsAroundInterceptor {
    private static final ILog logger = LogManager.getLogger(EurekaHealthCheckEnabledInterceptor.class);
    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        //如果不启用，就不执行内部逻辑，直接返回服务当前的健康状态。和eureka.client.eurekahealthcheck.enabled=false的效果一样
        if(!GovernanceConfig.EUREKA_HEALTH_CHECK_ENABLED){
            InstanceInfo.InstanceStatus status = (InstanceInfo.InstanceStatus) allArguments[0];
            result.defineReturnValue(status);
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        logger.error(t.getMessage(),t);
    }
}
