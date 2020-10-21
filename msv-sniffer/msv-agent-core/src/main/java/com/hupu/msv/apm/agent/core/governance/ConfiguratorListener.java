package com.hupu.msv.apm.agent.core.governance;


import com.hupu.msv.apm.network.governance.GovernanceConfigResponse;

/**
 * @author: zhaoxudong
 * @date: 2019-10-29 15:34
 * @description:
 */
public interface ConfiguratorListener {

    /**
     * 加载配置
     * @param response
     */
    void loadConfig(GovernanceConfigResponse response);
}
