package com.hupu.msv.apm.agent.core.governance.eurekahealthcheck;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hupu.msv.apm.agent.core.governance.ConfiguratorListener;
import com.hupu.msv.apm.agent.core.governance.GovernanceConfig;
import com.hupu.msv.apm.agent.core.governance.GovernanceConstants;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.network.governance.EurekaHealthCheck;
import com.hupu.msv.apm.network.governance.EurekaHealthCheckConfig;
import com.hupu.msv.apm.network.governance.GovernanceConfigResponse;

import java.lang.reflect.Field;
import java.util.*;

/**
 * @author: zhaoxudong
 * @date: 2020-02-13 19:41
 * @description: 健康检测配置
 */
public class EurekaHealthCheckConfigurator implements ConfiguratorListener {
    private static final ILog logger = LogManager.getLogger(EurekaHealthCheckConfigurator.class);
    private static final Gson GSON = new GsonBuilder().disableHtmlEscaping().create();
    private LinkedHashMap<Long, List<String>> itemMap = new LinkedHashMap<>();

    @Override
    public void loadConfig(GovernanceConfigResponse response) {
        if (!response.hasEurekaHealthCheck()) {
            updateVersion(response);
            return;
        }
        EurekaHealthCheck healthCheck = response.getEurekaHealthCheck();
        logger.info("接收到eureka health check 配置：{}", GSON.toJson(healthCheck));
        if ("true".equalsIgnoreCase(healthCheck.getEnabled())) {
            GovernanceConfig.EUREKA_HEALTH_CHECK_ENABLED = true;
        } else if ("false".equalsIgnoreCase(healthCheck.getEnabled())) {
            GovernanceConfig.EUREKA_HEALTH_CHECK_ENABLED = false;
        }
        if (healthCheck.getConfigCount() > 0) {
            for (EurekaHealthCheckConfig config : healthCheck.getConfigList()) {
                if (config.getDelFlag() == GovernanceConstants.RULE_DEL_FLAG_NORMAL
                        && config.getEnabled() == GovernanceConstants.RULE_ENABLED_OPEN) {
                    itemMap.put(config.getId(), config.getItemsList());
                    EurekaHealthCheckConstants.INFLUENCE_AVAILABILITY_NAMES.clear();
                    EurekaHealthCheckConstants.INFLUENCE_AVAILABILITY_NAMES.addAll(config.getItemsList());
                } else {
                    //如果是删除或者不启用操作，需要还原到最近一次正常检测设置
                    EurekaHealthCheckConstants.INFLUENCE_AVAILABILITY_NAMES.clear();
                    itemMap.remove(config.getId());
                    if (itemMap.isEmpty()) {
                        continue;
                    }
                    Map.Entry<Long, List<String>> entry = null;
                    try {
                        entry = getTailByReflection(itemMap);
                        EurekaHealthCheckConstants.INFLUENCE_AVAILABILITY_NAMES.addAll(entry.getValue());
                    } catch (NoSuchFieldException | IllegalAccessException e) {
                        logger.error(e.getMessage(), e);
                    }

                }
            }
        }

        updateVersion(response);
        logger.info("eureka health check 刷新完成 itemMap：{}",
                GSON.toJson(itemMap));
    }

    /**
     * 取最后一个值
     *
     * @param <K>
     * @param <V>
     * @param map
     * @return
     * @throws NoSuchFieldException
     * @throws IllegalAccessException
     */
    private Map.Entry<Long, List<String>> getTailByReflection(LinkedHashMap<Long, List<String>> map)
            throws NoSuchFieldException, IllegalAccessException {
        Field tail = map.getClass().getDeclaredField("tail");
        tail.setAccessible(true);
        return (Map.Entry<Long, List<String>>) tail.get(map);
    }

    private void updateVersion(GovernanceConfigResponse response) {
        GovernanceConfig.CURRENT_EUREKA_HEALTH_CHECK_VERSION = response.getGlobal().getVersion();
    }
}
