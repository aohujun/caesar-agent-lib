package com.hupu.msv.apm.plugin.hystrix.v1.properties;

import com.hupu.msv.apm.agent.core.governance.circuitbreaker.properties.CircuitBreakerProperties;
import com.hupu.msv.apm.agent.core.governance.circuitbreaker.properties.CircuitBreakerPropertiesCacheFactory;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesStrategy;

/**
 * @author chenbaochao
 * @version 0.0.1
 * @date 2019/11/14 5:42 下午
 */
public class HystrixPropertiesStrategyWrapper extends HystrixPropertiesStrategy {


    @Override
    public HystrixCommandProperties getCommandProperties(HystrixCommandKey commandKey, HystrixCommandProperties.Setter builder) {
        CircuitBreakerProperties circuitBreakerProperties = CircuitBreakerPropertiesCacheFactory.INSTANCE.get(commandKey.name());
        if (circuitBreakerProperties != null) {
            return (HystrixCommandProperties) circuitBreakerProperties;
        }
        if (builder == null) {
            builder = com.netflix.hystrix.HystrixCommandProperties.Setter();
        }
        HystrixCommandProperties properties = new HystrixCommandProperties(commandKey, builder);
        CircuitBreakerPropertiesCacheFactory.INSTANCE.addCacheRule(commandKey.name(), properties);
        return properties;
    }


    /**
     * 这里忽略掉hystrix自身的commandProperties缓存
     *
     * @param commandKey
     * @param builder
     * @return
     */
    @Override
    public String getCommandPropertiesCacheKey(HystrixCommandKey commandKey, HystrixCommandProperties.Setter builder) {
        return null;
    }


}
