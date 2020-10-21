package com.hupu.msv.apm.plugin.health.indicator.v1;

import com.hupu.msv.apm.agent.core.governance.GovernanceEventEnum;
import com.hupu.msv.apm.agent.core.governance.GovernanceEventStatusEnum;
import com.hupu.msv.apm.agent.core.governance.eurekahealthcheck.EurekaHealthCheckConstants;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.plugin.governance.common.GovernanceReportUtil;
import com.hupu.msv.apm.plugin.health.indicator.v1.common.HealthIndicatorHandler;
import org.springframework.boot.actuate.health.*;
import org.springframework.cloud.netflix.eureka.EurekaHealthCheckHandler;
import org.springframework.util.StringUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author: zhaoxudong
 * @date: 2020-02-12 13:34
 * @description: eureka client health check只检测影响服务可用性的项目，其他的忽略
 */
public class EurekaHealthCheckExcludeInterceptor implements InstanceMethodsAroundInterceptor {
    private static final ILog logger = LogManager.getLogger(EurekaHealthCheckExcludeInterceptor.class);

    /**
     * 检测次数
     */
    private Map<String, Integer> itemCheckCount = new HashMap<>();
    /**
     * 项目的稳定健康状态
     */
    private Map<String, Health> repeatedStabilizationCheckResult = new HashMap<>();
    /**
     * 需要多次校验时，每个项目的最近一次结果都会放进去
     */
    private Map<String, String> repeatedLatestCheckResult = new HashMap<>();

    /**
     * 上次检测的状态结果
     */
    private Map<String, String> latestCheckResult = new HashMap<>();

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {

        try {
            if (EurekaHealthCheckHandler.class.isAssignableFrom(objInst.getClass())) {
                EurekaHealthCheckHandler handler = (EurekaHealthCheckHandler) objInst;
                try {
                    Field field = EurekaHealthCheckHandler.class.getDeclaredField("healthIndicator");
                    field.setAccessible(true);
                    CompositeHealthIndicator healthIndicator = (CompositeHealthIndicator) field.get(handler);
                    Field healthIndicatorIndicators = CompositeHealthIndicator.class.getDeclaredField("indicators");
                    healthIndicatorIndicators.setAccessible(true);
                    Map<String, HealthIndicator> indicators = (Map<String, HealthIndicator>) healthIndicatorIndicators.get(healthIndicator);
                    Field healthIndicatorHealthAggregator = CompositeHealthIndicator.class.getDeclaredField("healthAggregator");
                    healthIndicatorHealthAggregator.setAccessible(true);
                    HealthAggregator healthAggregator = (HealthAggregator) healthIndicatorHealthAggregator.get(healthIndicator);
                    Map<String, Health> healths = new LinkedHashMap<>();
                    for (Map.Entry<String, HealthIndicator> entry : indicators.entrySet()) {
                        if (StringUtils.isEmpty(entry)) {
                            continue;
                        }

                        String key = HealthIndicatorHandler.getKey(entry.getKey());
                        int index = getItemIndex(key);
                        if (index == -1) {
                            // 跳过不影响可用性的项目
                            repeatedStabilizationCheckResult.remove(key);
                            itemCheckCount.remove(key);
                            continue;
                        }
                        String[] items = EurekaHealthCheckConstants.INFLUENCE_AVAILABILITY_NAMES.get(index).split(":");
                        String item = HealthIndicatorHandler.getKey(items[0]);
                        if (isCheckOnce(items)) {
                            // 只校验一次
                            healths.put(item, entry.getValue().health());
                            reportResult(item, entry.getValue().health().getStatus().getCode());
                            repeatedStabilizationCheckResult.remove(item);
                            itemCheckCount.remove(item);
                            continue;
                        }
                        repeatedCheck(healths, entry, items[1], item);
                    }
                    Status status;
                    if (healths.isEmpty()) {
                        //healths是空时，表示没有需要检测的项目，应用默认为UP状态
                        status = Status.UP;
                    } else {
                        status = healthAggregator.aggregate(healths).getStatus();
                    }
                    Method mapToInstanceStatus = EurekaHealthCheckHandler.class.getDeclaredMethod(
                            "mapToInstanceStatus", Status.class);
                    mapToInstanceStatus.setAccessible(true);
                    result.defineReturnValue(mapToInstanceStatus.invoke(handler, status));
                } catch (NoSuchFieldException | IllegalAccessException e) {
                    logger.error(e.getMessage(), e);
                    throw e;
                }
            }
        } catch (Exception e) {
            logger.error(e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 上报结果到普罗米修斯
     *
     * @param item
     * @param statusCode
     */
    private void reportResult(String item, String statusCode) {
        if (latestCheckResult.get(item) == null || !latestCheckResult.get(item).equalsIgnoreCase(statusCode)) {
            logger.info("eureka health check状态发生变化。检测项目：{},之前状态：{},当前状态：{}",
                    item,latestCheckResult.get(item),statusCode);
            latestCheckResult.put(item, statusCode);
            GovernanceReportUtil.recordGauge(GovernanceEventEnum.EUREKA_HEALTH_CHECK, item,
                    statusCode.equalsIgnoreCase("up") ? GovernanceEventStatusEnum.OPEN : GovernanceEventStatusEnum.CLOSE);
        }

    }

    /**
     * 多次检测逻辑
     *
     * @param healths
     * @param entry
     * @param checkCountString
     * @param checkItem
     */
    private void repeatedCheck(Map<String, Health> healths, Map.Entry<String, HealthIndicator> entry, String checkCountString, String checkItem) {
        int count = Integer.valueOf(checkCountString.trim());
        //获取计数
        Integer checkCount = itemCheckCount.getOrDefault(checkItem, 0);
        //获取稳定结果
        Health stabilizationHealth = repeatedStabilizationCheckResult.get(checkItem);
        String stabilizationStatusCode = stabilizationHealth == null ? null : stabilizationHealth.getStatus().getCode();
        //本次校验
        Health currentHealth = entry.getValue().health();
        String currentStatusCode = currentHealth.getStatus().getCode();
        if (stabilizationStatusCode == null) {
            //稳定状态为null，就放入当前状态
            repeatedStabilizationCheckResult.put(checkItem, currentHealth);
            checkCount++;
        } else if (!currentStatusCode.equals(repeatedLatestCheckResult.get(checkItem))) {
            //和上次检测结果不相同，重置次数
            checkCount = 1;
        } else {
            checkCount++;
        }
        if (checkCount >= count) {
            //校验次数达标就放进结果map
            healths.put(checkItem, currentHealth);
            //将次数重置,防止溢出
            checkCount = 0;
            //设置稳定状态
            repeatedStabilizationCheckResult.put(checkItem, currentHealth);
            //上报结果
            reportResult(checkItem, currentHealth.getStatus().getCode());
        } else if (stabilizationHealth != null) {
            //校验次数不够就放稳定结果
            healths.put(checkItem, stabilizationHealth);
        }
        itemCheckCount.put(checkItem, checkCount);
        repeatedLatestCheckResult.put(checkItem, currentStatusCode);
    }

    private boolean isCheckOnce(String[] keys) {
        //只有检测名称
        if (keys.length == 1) {
            return true;
        }
        //格式不符合要求
        if (keys.length > 2) {
            return true;
        }
        //次数小于等于1
        try {
            if (Integer.valueOf(keys[1].trim()) <= 1) {
                return true;
            }
        } catch (NumberFormatException e) {
            logger.error(e.getMessage(), e);
        }
        return false;
    }

    /**
     * 是否影响服务可用性
     *
     * @param key 检测名称
     * @return
     */
    private int getItemIndex(String key) {
        int size = EurekaHealthCheckConstants.INFLUENCE_AVAILABILITY_NAMES.size();
        int index = -1;
        String name;
        for (int i = 0; i < size; i++) {
            name = EurekaHealthCheckConstants.INFLUENCE_AVAILABILITY_NAMES.get(i).split(":")[0];
            if (key.equalsIgnoreCase(name)) {
                index = i;
                break;
            }
        }

        return index;
    }


    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Object ret) throws Throwable {
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes, Throwable t) {
        logger.error(t.getMessage(), t);
    }
}
