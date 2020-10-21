package com.hupu.msv.apm.agent.core.governance.gray;

import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.hupu.msv.apm.agent.core.governance.ConfiguratorListener;
import com.hupu.msv.apm.agent.core.governance.GovernanceConfig;
import com.hupu.msv.apm.agent.core.governance.gray.common.GrayRouteManager;
import com.hupu.msv.apm.agent.core.governance.gray.config.GrayRuleConfig;
import com.hupu.msv.apm.agent.core.governance.gray.utils.EmptyUtils;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.network.governance.*;

import java.util.*;

import static com.hupu.msv.apm.agent.core.governance.GovernanceConstants.RULE_DEL_FLAG_DEL;
import static com.hupu.msv.apm.agent.core.governance.GovernanceConstants.RULE_ENABLED_CLOSE;

/**
 * @author: Aile
 * @date: 2019-10-29 16:57
 * @description:
 */
public class GrayConfigurator implements ConfiguratorListener {

    private static final ILog log = LogManager.getLogger(GrayConfigurator.class);
    /**
     * 独占式实例配置
     */
    private HashMap<Long, HashMap<String, Set<String>>> exclusiveInstanceConfigCache = new HashMap<>();
    private HashMap<Long, List<HashMap<String, String>>> extensionHttpHeadersConfigCache = new LinkedHashMap<>();

    @Override
    public void loadConfig(GovernanceConfigResponse response) {
        if (!response.hasGray()) {
            GovernanceConfig.CURRENT_GRAY_VERSION = response.getGlobal().getVersion();
            return;
        }

        Gray gray = response.getGray();
        if ("true".equalsIgnoreCase(gray.getEnabled())) {
            GovernanceConfig.GRAY_ENABLED = true;
        } else if ("false".equalsIgnoreCase(gray.getEnabled())) {
            GovernanceConfig.GRAY_ENABLED = false;
        }

        boolean success = true;
        if (EmptyUtils.isNotEmpty(response.getGray().getConfigList())) {
            success = success && refreshRouteStrategyMap(response.getGray().getConfigList());
        }

        if (EmptyUtils.isNotEmpty(response.getGray().getExclusiveConfigList())) {
            success = success && refreshExclusiveInstanceList(response.getGray().getExclusiveConfigList());
        }

        if (EmptyUtils.isNotEmpty(response.getGray().getExtensionHttpHeadersConfigList())) {
            success = success && refreshExtensionHttpHeaders(response.getGray().getExtensionHttpHeadersConfigList());
        }

        //更新当前版本号
        if (success) {
            GovernanceConfig.CURRENT_GRAY_VERSION = response.getGlobal().getVersion();
        }
    }

    /**
     * 刷新需要透传的headers
     *
     * @param extensionHttpHeadersConfigList
     * @return
     */
    private boolean refreshExtensionHttpHeaders(List<ExtensionHttpHeadersConfig> extensionHttpHeadersConfigList) {
        boolean success = true;
        for (ExtensionHttpHeadersConfig config : extensionHttpHeadersConfigList) {
            if (config.getDelFlag() == RULE_DEL_FLAG_DEL || config.getEnabled() == RULE_ENABLED_CLOSE) {
                extensionHttpHeadersConfigCache.remove(config.getId());
            } else {
                String headers = config.getHeaderList();
                if (EmptyUtils.isEmpty(headers)) {
                    extensionHttpHeadersConfigCache.remove(config.getId());
                    continue;
                }
                try {
                    List<HashMap<String, String>> newHeaders = new GsonBuilder().create().fromJson(headers,
                            new TypeToken<List<HashMap<String, String>>>() {
                            }.getType());
                    newHeaders.forEach(map->{
                        map.put("id",String.valueOf(config.getId()));
                        map.put("color",config.getColor());
                    });
                    extensionHttpHeadersConfigCache.put(config.getId(), newHeaders);
                    GrayRouteManager.INSTANCE.setCover(config.getCover());
                } catch (Exception e) {
                    log.error(e, "gray config is illegal: strategyId={}  data={}", config.getId(), config);
                    success = false;
                }
            }
        }
        //最终需要透传的headers
        Map<String, HashMap<String, String>> finalHeaders = new HashMap<>();
        // header去重，已最近更新的覆盖之前设置
        extensionHttpHeadersConfigCache.values().forEach(hashMaps -> {
            hashMaps.forEach(hashMap -> {
                finalHeaders.put(hashMap.get("key"),hashMap);
            });
        });
        if(finalHeaders.isEmpty()){
            GrayRouteManager.INSTANCE.setCover(false);
        }
        GrayRouteManager.INSTANCE.getGrayCommonConfig().setExtensionHttpHeaders(finalHeaders.values());
        log.info("refresh ExtensionHttpHeaders , config={}", new GsonBuilder().disableHtmlEscaping().create().toJson(GrayRouteManager.INSTANCE.getGrayCommonConfig().getExtensionHttpHeaders()));
        return success;
    }


    /**
     * 刷新灰度路由策略
     *
     * @param grayConfigs
     */
    private boolean refreshRouteStrategyMap(List<GrayConfig> grayConfigs) {

        boolean success = true;
        for (GrayConfig grayConfig : grayConfigs) {

            if (grayConfig.getDelFlag() == RULE_DEL_FLAG_DEL || grayConfig.getEnabled() == RULE_ENABLED_CLOSE) {
                GrayRouteManager.INSTANCE.getGrayRuleConfigMap().remove(grayConfig.getId());

            } else {
                String config = grayConfig.getConfig();
                if (EmptyUtils.isEmpty(config)) {
                    GrayRouteManager.INSTANCE.getGrayRuleConfigMap().put(grayConfig.getId(), null);
                    continue;
                }
                try {
                    GrayRuleConfig strategyItem = new GsonBuilder().create().fromJson(config, GrayRuleConfig.class);
                    strategyItem.setId(grayConfig.getId());
                    GrayRouteManager.INSTANCE.getGrayRuleConfigMap().put(grayConfig.getId(), strategyItem);
                } catch (Exception e) {
                    log.error(e, "gray config is illegal: strategyId={}  data={}", grayConfig.getId(), config);
                    success = false;
                }
            }
        }

        try {
            GrayRouteManager.INSTANCE.getGrayRouteStrategy().initRouteTable(GrayRouteManager.INSTANCE.getGrayRuleConfigMap());
        } catch (Exception e) {
            log.error("init route strategy error", e);
            success = false;
        }
        return success;
    }

    /**
     * 刷新独占式实例列表
     */
    private boolean refreshExclusiveInstanceList(List<ExclusiveConfig> exclusiveConfigs) {

        boolean success = true;
        for (ExclusiveConfig exclusiveConfig : exclusiveConfigs) {

            if (exclusiveConfig.getDelFlag() == RULE_DEL_FLAG_DEL || exclusiveConfig.getEnabled() == RULE_ENABLED_CLOSE) {
                exclusiveInstanceConfigCache.remove(exclusiveConfig.getId());
            } else {

                String exclusiveInstances = exclusiveConfig.getInstanceIdMap();
                if (EmptyUtils.isEmpty(exclusiveInstances)) {
                    exclusiveInstanceConfigCache.remove(exclusiveConfig.getId());
                    continue;
                }
                try {
                    HashMap<String, Set<String>> newExclusiveInstance = new GsonBuilder().create().fromJson(exclusiveInstances,
                            new TypeToken<HashMap<String, Set<String>>>() {
                            }.getType());
                    exclusiveInstanceConfigCache.put(exclusiveConfig.getId(), newExclusiveInstance);
                } catch (Exception e) {
                    log.error(e, "gray config is illegal: strategyId={}  data={}", exclusiveConfig.getId(), exclusiveInstances);
                    success = false;
                }
            }
        }

        //独占式实例配置生效。
        HashMap<String, Set<String>> exclusiveInstanceIdMap = new HashMap<>();
        exclusiveInstanceConfigCache.values().forEach((value) -> {
            if (EmptyUtils.isEmpty(value)) {
                return;
            }
            value.forEach((k, v) -> {
                if (EmptyUtils.isEmpty(k) || EmptyUtils.isEmpty(v)) {
                    return;
                }

                if (exclusiveInstanceIdMap.get(k) == null) {
                    exclusiveInstanceIdMap.put(k, v);
                } else {
                    exclusiveInstanceIdMap.get(k).addAll(v);
                }

            });
        });

        GrayRouteManager.INSTANCE.getGrayCommonConfig().setExclusiveInstanceMap(exclusiveInstanceIdMap);
        log.info("refresh exclusiveInstance , config={}", new GsonBuilder().disableHtmlEscaping().create().toJson(GrayRouteManager.INSTANCE.getGrayCommonConfig().getExclusiveInstanceMap()));
        return success;
    }
}
