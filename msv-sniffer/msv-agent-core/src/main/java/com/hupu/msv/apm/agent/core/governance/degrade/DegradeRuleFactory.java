package com.hupu.msv.apm.agent.core.governance.degrade;

import com.hupu.msv.apm.agent.core.governance.GovernanceConstants;
import com.hupu.msv.apm.agent.core.governance.traffic.TrafficConfigurator;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.network.governance.DegradeConfig;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author: zhaoxudong
 * @date: 2019-11-26 10:54
 * @description:
 */
public enum DegradeRuleFactory {
    INSTANCE;
    //key:接口路径 ，value：详细规则
    private Map<String, DegradeConfig> pathRule = new ConcurrentHashMap<>();
    //key:规则唯一标识id，value：详细规则
    private Map<Long, DegradeConfig> degradeConfigMap = new ConcurrentHashMap<>();

    /**
     * 加载配置
     *
     * @param configList
     */
    public void load(List<DegradeConfig> configList) {
        boolean isChange = false;
        //增量整理规则
        for (DegradeConfig config : configList) {
            if (GovernanceConstants.RULE_DEL_FLAG_NORMAL == config.getDelFlag()
                    && GovernanceConstants.RULE_ENABLED_OPEN == config.getEnabled()) {
                //add
                degradeConfigMap.put(config.getId(), config);
                isChange = true;
            } else {
                //remove
                DegradeConfig degradeConfig = degradeConfigMap.remove(config.getId());
                if (degradeConfig != null) {
                    isChange = true;
                }
            }
        }
        //全量设置
        if (isChange) {
            pathRule.clear();
            degradeConfigMap.values().forEach(degradeConfig -> {
                Arrays.stream(degradeConfig.getPath().split(",")).forEach(path -> {

                    pathRule.put(path, degradeConfig);
                });
            });
        }

    }

    /**
     * 获取规则
     *
     * @param key 接口路径
     * @return
     */
    public DegradeConfig getRule(String key) {
        return pathRule.get(key);
    }
}
