package com.hupu.msv.apm.plugin.kafka;

import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.plugin.kafka.util.KafkaClassUtil;

import java.lang.reflect.Method;

/**
 * @author: edison.li
 * @date: 2020/7/13 6:38 下午
 * @description:
 */
public class ApiVersionsInterceptor implements InstanceMethodsAroundInterceptor {

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {

    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {

        byte maxUsableProduceMagic = (Byte) KafkaClassUtil.getObjectField(objInst,"maxUsableProduceMagic");
        KafkaClassUtil.MAX_USABLE_PRODUCE_MAGIC = maxUsableProduceMagic;

        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {

    }
}
