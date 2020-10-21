package com.hupu.msv.apm.plugin.health.indicator.v1;

import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import com.hupu.msv.apm.plugin.health.indicator.v1.common.HealthIndicatorHandler;
import com.hupu.msv.apm.plugin.health.indicator.v1.ext.TomcatThreadHealthIndicator;
import org.springframework.boot.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.actuate.health.CompositeHealthIndicator;
import org.springframework.cloud.netflix.eureka.EurekaHealthCheckHandler;

import java.lang.reflect.Field;
import java.util.Locale;

/**
 * @author: zhaoxudong
 * @date: 2020-01-16 14:05
 * @description: 构建eureka health时加入自定义健康检测项目
 * *
 */
public class EurekaHealthCheckHandlerConstructorInterceptor implements InstanceConstructorInterceptor {
    private static final ILog logger = LogManager.getLogger(EurekaHealthCheckHandlerConstructorInterceptor.class);
    private static TomcatThreadHealthIndicator tomcatThreadHealthIndicator;

    @Override
    public void onConstruct(EnhancedInstance enhancedInstance, Object[] allArguments) {
        //加入eureka client健康检测端点。决定eureka上的服务状态是down还是up
        if (EurekaHealthCheckHandler.class.isAssignableFrom(enhancedInstance.getClass())) {
            EurekaHealthCheckHandler healthCheckHandler = (EurekaHealthCheckHandler) enhancedInstance;
            try {
                Field field = EurekaHealthCheckHandler.class.getDeclaredField("healthIndicator");
                field.setAccessible(true);
                CompositeHealthIndicator healthIndicator = (CompositeHealthIndicator) field.get(healthCheckHandler);
                HealthIndicatorHandler.getIndicatorMap().forEach((k, v) ->
                        healthIndicator.addHealthIndicator(HealthIndicatorHandler.getKey(k), v)
                );
                field.set(healthCheckHandler, healthIndicator);
            } catch (NoSuchFieldException | IllegalAccessException e) {
                logger.error(e.getMessage(), e);
            }

        }

    }

}

