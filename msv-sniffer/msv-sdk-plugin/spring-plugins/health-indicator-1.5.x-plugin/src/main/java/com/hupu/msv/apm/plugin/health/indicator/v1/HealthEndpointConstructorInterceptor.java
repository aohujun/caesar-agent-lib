package com.hupu.msv.apm.plugin.health.indicator.v1;

import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import com.hupu.msv.apm.plugin.health.indicator.v1.common.HealthIndicatorHandler;
import com.hupu.msv.apm.plugin.health.indicator.v1.ext.TomcatThreadHealthIndicator;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;

import java.lang.reflect.Field;

/**
 * @author: zhaoxudong
 * @date: 2020-01-16 14:05
 * @description: 构建eureka health时加入自定义健康检测项目
 * *
 */
public class HealthEndpointConstructorInterceptor implements InstanceConstructorInterceptor {
    private static final ILog logger = LogManager.getLogger(HealthEndpointConstructorInterceptor.class);
    private static TomcatThreadHealthIndicator tomcatThreadHealthIndicator;

    @Override
    public void onConstruct(EnhancedInstance enhancedInstance, Object[] allArguments) {
        //加入health端点。调用/health接口时可以看到
        if (HealthEndpoint.class.isAssignableFrom(enhancedInstance.getClass())) {
            HealthEndpoint healthEndpoint = (HealthEndpoint) enhancedInstance;
            try {
                Field field = HealthEndpoint.class.getDeclaredField("healthIndicator");
                field.setAccessible(true);
                CompositeHealthIndicator healthIndicator = (CompositeHealthIndicator) field.get(healthEndpoint);
                //可以加入更多的健康检测项目
                HealthIndicatorHandler.getIndicatorMap().forEach((k, v) ->
                        healthIndicator.addHealthIndicator(HealthIndicatorHandler.getKey(k), v)
                );
                field.set(healthEndpoint, healthIndicator);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error(e.getMessage(), e);
            }
        }
    }



}
