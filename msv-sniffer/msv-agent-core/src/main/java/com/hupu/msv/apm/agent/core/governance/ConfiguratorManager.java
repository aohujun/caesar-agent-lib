package com.hupu.msv.apm.agent.core.governance;


import com.hupu.msv.apm.network.governance.GovernanceConfigResponse;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * @author: zhaoxudong
 * @date: 2019-10-29 15:08
 * @description:
 */
public enum ConfiguratorManager {
    INSTANCE;
    private List<ConfiguratorListener> listeners = Collections.synchronizedList(new LinkedList<ConfiguratorListener>());

    public void addListener(ConfiguratorListener listener) {
        listeners.add(listener);
    }

    public void loadConfig(GovernanceConfigResponse response){
        if(listeners.isEmpty()){
            return;
        }
        for(ConfiguratorListener listener:listeners){
            listener.loadConfig(response);
        }
    }
}
