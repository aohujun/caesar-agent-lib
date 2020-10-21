package com.hupu.msv.apm.plugin.spring.cloud.eureka.client;

import com.hupu.msv.apm.agent.core.governance.GovernanceConstants;
import com.hupu.msv.apm.agent.core.governance.gray.common.GrayRouteManager;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import com.hupu.msv.apm.agent.core.util.CaesarAgentUtil;
import com.netflix.appinfo.EurekaInstanceConfig;


/**
 * @description: 设置meta-data
 * @author: Aile
 * @create: 2020/03/06 15:44
 */
public class EurekaRegistrationInterceptor implements InstanceConstructorInterceptor {

    private static final ILog log = LogManager.getLogger(EurekaRegistrationInterceptor.class);

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        try {
            EurekaInstanceConfig eurekaInstanceConfig = (EurekaInstanceConfig) allArguments[0];
            eurekaInstanceConfig.getMetadataMap().put("msv-agent-version", CaesarAgentUtil.getVersion(GovernanceConstants.CAESAR_DEFAULT_VERSION));
            eurekaInstanceConfig.getMetadataMap().putAll(GrayRouteManager.getInstanceMetadata());
        } catch (Exception e) {
            log.error(" EurekaRegistration enhance false", e);
        }

    }
}
