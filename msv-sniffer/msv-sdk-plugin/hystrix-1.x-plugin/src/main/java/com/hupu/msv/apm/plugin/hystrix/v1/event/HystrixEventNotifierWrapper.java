package com.hupu.msv.apm.plugin.hystrix.v1.event;

import com.hupu.msv.apm.agent.core.governance.GovernanceEventEnum;
import com.hupu.msv.apm.agent.core.governance.GovernanceEventStatusEnum;
import com.hupu.msv.apm.plugin.governance.common.GovernanceReportUtil;
import com.netflix.hystrix.HystrixCircuitBreaker;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixEventType;
import com.netflix.hystrix.strategy.eventnotifier.HystrixEventNotifier;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author chenbaochao
 * @version 0.0.1
 * @date 2019/11/18 5:12 上午
 */
public class HystrixEventNotifierWrapper extends HystrixEventNotifier {


    private static Map<HystrixCommandKey, HystrixCircuitBreaker> circuitBreakerRecord = new ConcurrentHashMap<>();

    /**
     * run()
     * SUCCESS：run()成功，不触发getFallback()
     * FAILURE：run()抛异常，触发getFallback()
     * TIMEOUT：run()超时，触发getFallback()
     * BAD_REQUEST：run()抛出HystrixBadRequestException，不触发getFallback()
     * SHORT_CIRCUITED：断路器开路，触发getFallback()
     * THREAD_POOL_REJECTED：线程池耗尽，触发getFallback()
     * FALLBACK_MISSING：没有实现getFallback()，抛出异常
     * getFallback()
     * FALLBACK_SUCCESS：getFallback()成功，不抛异常
     * FALLBACK_FAILURE：getFallback()失败，抛异常
     * FALLBACK_REJECTION：调用getFallback()的线程数超量，抛异常
     *
     * @param eventType
     * @param key
     */

    @Override
    public void markEvent(HystrixEventType eventType, HystrixCommandKey key) {
        if (circuitBreakerRecord.containsKey(key)) {
            HystrixCircuitBreaker circuitBreaker = circuitBreakerRecord.get(key);
            if (circuitBreaker.allowRequest()) {
                circuitBreakerRecord.remove(key);
                //熔断恢复  上报事件
                GovernanceReportUtil.recordGauge(GovernanceEventEnum.CIRCUITBREAKER,key.name(), GovernanceEventStatusEnum.CLOSE);
            }
        } else {
            HystrixCircuitBreaker circuitBreaker = HystrixCircuitBreaker.Factory.getInstance(key);

            if (circuitBreaker != null && circuitBreaker.isOpen()) {
                circuitBreakerRecord.put(key, circuitBreaker);
                //开启熔断 上报事件
                GovernanceReportUtil.recordCount(GovernanceEventEnum.CIRCUITBREAKER,key.name());
                GovernanceReportUtil.recordGauge(GovernanceEventEnum.CIRCUITBREAKER,key.name(), GovernanceEventStatusEnum.OPEN);

            }
        }

        switch (eventType) {
            case SHORT_CIRCUITED:
                //System.out.println("统计被熔断的请求数");
                break;
            case TIMEOUT:
                //System.out.println("统计超时断开的请求数");
                break;
        }

    }

    @Override
    public void markCommandExecution(HystrixCommandKey key, HystrixCommandProperties.ExecutionIsolationStrategy isolationStrategy, int duration, List<HystrixEventType> eventsDuringExecution) {

    }
}
