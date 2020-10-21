package com.hupu.msv.apm.plugin.hystrix.v1.properties;

import com.hupu.msv.apm.agent.core.governance.GovernanceConfig;
import com.hupu.msv.apm.agent.core.governance.circuitbreaker.properties.CircuitBreakerProperties;
import com.hupu.msv.apm.network.governance.CircuitBreakerConfig;
import com.hupu.msv.apm.plugin.hystrix.v1.factory.MatchedRelationRuleFactory;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.strategy.properties.HystrixProperty;


/**
 * @author chenbaochao
 * @version 0.0.1
 * @date 2019/11/14 6:07 下午
 */
public class HystrixCommandProperties extends com.netflix.hystrix.HystrixCommandProperties implements CircuitBreakerProperties {

    private HystrixCommandKey key;

//    private CircuitBreakerConfig circuitBreakerConfig;

    protected HystrixCommandProperties(HystrixCommandKey key, Setter setter) {
        super(key, setter);
        this.key = key;
//        this.circuitBreakerConfig = MatchedRelationRuleFactory.getInstance().getCircuitBreakerConfig(key.name());
    }


    @Override
    public HystrixProperty<Boolean> circuitBreakerEnabled() {
        return HystrixProperty.Factory.asProperty(GovernanceConfig.CIRCUIT_BREAKER_ENABLED);
    }

    @Override
    public HystrixProperty<Boolean> circuitBreakerForceOpen() {
        if (getCircuitBreakerConfig() != null) {
            return HystrixProperty.Factory.asProperty(getCircuitBreakerConfig().getForceOpen() == 1);
        }
        return super.circuitBreakerForceOpen();
    }

    @Override
    public HystrixProperty<Integer> executionTimeoutInMilliseconds() {
        if (getCircuitBreakerConfig() != null) {
            if (getCircuitBreakerConfig().getType() == 2 || getCircuitBreakerConfig().getType() == 5) {
                return HystrixProperty.Factory.asProperty(getCircuitBreakerConfig().getThreshold());
            }
        }
        return super.executionTimeoutInMilliseconds();
    }

    @Override
    public HystrixProperty<Integer> metricsRollingStatisticalWindowInMilliseconds() {
        if (getCircuitBreakerConfig() != null&&getCircuitBreakerConfig().getTimeWindow() > 0) {
            return HystrixProperty.Factory.asProperty(getCircuitBreakerConfig().getTimeWindow());
        }
        return super.metricsRollingStatisticalWindowInMilliseconds();
    }

    @Override
    public HystrixProperty<Integer> circuitBreakerRequestVolumeThreshold() {
        if (getCircuitBreakerConfig() != null) {
            return HystrixProperty.Factory.asProperty(getCircuitBreakerConfig().getMinRequestNumber());
        }
        return super.circuitBreakerRequestVolumeThreshold();
    }

    @Override
    public HystrixProperty<Integer> circuitBreakerErrorThresholdPercentage() {
        if (getCircuitBreakerConfig() != null && getCircuitBreakerConfig().getStatisticalType() == 1) {
            return HystrixProperty.Factory.asProperty(getCircuitBreakerConfig().getCondition());
        }
        return super.circuitBreakerErrorThresholdPercentage();
    }

    @Override
    public HystrixProperty<Integer> circuitBreakerSleepWindowInMilliseconds() {
        if (getCircuitBreakerConfig() != null) {
            return HystrixProperty.Factory.asProperty(getCircuitBreakerConfig().getSleepWindow());
        }
        return super.circuitBreakerSleepWindowInMilliseconds();
    }

    public CircuitBreakerConfig getCircuitBreakerConfig() {
        return MatchedRelationRuleFactory.getInstance().getCircuitBreakerConfig(key.name());
    }


}
