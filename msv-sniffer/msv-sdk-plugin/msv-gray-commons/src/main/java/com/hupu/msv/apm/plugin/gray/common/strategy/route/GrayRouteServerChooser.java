
package com.hupu.msv.apm.plugin.gray.common.strategy.route;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.hupu.msv.apm.agent.core.governance.gray.common.GrayRouteManager;
import com.hupu.msv.apm.agent.core.governance.gray.config.GrayTag;
import com.hupu.msv.apm.agent.core.governance.gray.utils.EmptyUtils;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.netflix.appinfo.InstanceInfo;
import com.netflix.loadbalancer.Server;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.*;

/**
 * @description: 灰度路由实例列表选择器 ： 根据 tag选择。
 * @author: Aile
 * @create: 2019/11/06 15:44
 */
public class GrayRouteServerChooser implements RouteServerChooser {

    private static final ILog logger = LogManager.getLogger(GrayRouteServerChooser.class);

    private Gson gson = new GsonBuilder().serializeNulls().disableHtmlEscaping().create();

    public static final String IP_ADDR = "ipAddr";

    @Override
    public List<Server> choose(List<Server> servers) {

        if (EmptyUtils.isEmpty(servers)) {
            logger.warn("not found server list!");
            return new ArrayList();
        }

        String appName = servers.get(0).getMetaInfo().getAppName().toLowerCase();
        if (GrayRouteManager.INSTANCE.getContextGrayRule(appName) == null) {
            return excludeExclusiveInstance(servers, GrayRouteManager.INSTANCE.getExclusiveInstance(appName));
        }
        return chooseByTag(appName, servers);
    }


    /**
     * 过滤掉独占式实例
     *
     * @param servers
     * @param exclusiveInstance
     * @return
     */
    private List<Server> excludeExclusiveInstance(List<Server> servers, Set<String> exclusiveInstance) {

        if (EmptyUtils.isEmpty(exclusiveInstance)) {
            return servers;
        }

        List<Server> targetServers = new ArrayList<>();
        for (Server server : servers) {
            String ip = ((DiscoveryEnabledServer) server).getInstanceInfo().getIPAddr();
            if (!exclusiveInstance.contains(ip)) {
                targetServers.add(server);
            }
        }
        return targetServers;
    }


    /**
     * 过滤出指定 metadata 的 server
     *
     * @param appName
     * @param servers
     * @return
     */
    private List<Server> chooseByTag(String appName, List<Server> servers) {

        GrayTag grayTag = GrayRouteManager.INSTANCE.getContextGrayRule(appName);
        if (grayTag == null) {
            return servers;
        }

        List<Server> targetServers = new ArrayList<>();

        if (EmptyUtils.isEmpty(grayTag.getWeightSoFars())) {
            //无权重
            return chooseServerByMetaData(grayTag.getKey(), grayTag.getValues(), servers);
        } else {
            //根据权重选择实例列表
            List<String> values = grayTag.getValues();
            List<Integer> weightSoFars = grayTag.getWeightSoFars();

            if (EmptyUtils.isEmpty(values) || EmptyUtils.isEmpty(weightSoFars)) {
                logger.warn("gray route meta-data is empty!");
                return servers;
            }

            //根据权重，获取命中的实例列表
            while (!weightSoFars.isEmpty()) {

                int index = WeightUtils.choose(weightSoFars);
                if (index < 0) {
                    logger.warn("gray route config calculate wight error ：config={}", gson.toJson(grayTag));
                    return new ArrayList<>();
                }
                if (index >= values.size()) {
                    logger.warn("gray route config is illegal ：config={}", gson.toJson(grayTag));
                    return new ArrayList<>();
                }

                List<Server> results = chooseServerByMetaData(grayTag.getKey(), values.get(index), servers);
                results = statusFilter(results, InstanceInfo.InstanceStatus.UP);
                if (!results.isEmpty() || values.size() == 1) {
                    return results;
                }

                if (values == grayTag.getValues()) {
                    values = new ArrayList<>();
                    values.addAll(grayTag.getValues());

                    weightSoFars = new ArrayList<>();
                    weightSoFars.addAll(grayTag.getWeightSoFars());
                }

                //如果该meta data value对应的实例列表不存在，则重置 value 和 权重，重新根据权重获取实例列表。
                int subWeight;
                if (index > 0) {
                    subWeight = weightSoFars.get(index) - weightSoFars.get(index - 1);
                } else {
                    subWeight = weightSoFars.get(index);
                }
                values.remove(index);
                weightSoFars.remove(index);
                for (int i = index; i < weightSoFars.size(); i++) {
                    weightSoFars.set(i, weightSoFars.get(i) - subWeight);
                }
            }


        }


        return targetServers;
    }


    private List<Server> chooseServerByMetaData(String metaDataKey, String metaDataValue, List<Server> servers) {

        if (StringUtils.isEmpty(metaDataKey) || StringUtils.isEmpty(metaDataValue)) {
            return new ArrayList<>();
        }


        Iterator<Server> serverIterator = servers.iterator();
        List<Server> results = new ArrayList<>();


        if (IP_ADDR.equals(metaDataKey)) {
            for (Server server : servers) {
                DiscoveryEnabledServer discoveryEnabledServer = (DiscoveryEnabledServer) server;
                if (metaDataValue.equals(discoveryEnabledServer.getInstanceInfo().getIPAddr())) {
                    results.add(server);
                }
            }
            return results;
        }


        while (serverIterator.hasNext()) {
            DiscoveryEnabledServer server = (DiscoveryEnabledServer) serverIterator.next();
            Map<String, String> metaData = server.getInstanceInfo().getMetadata();
            boolean isMatched;
            //未知版本，未配置版本的实例。
            if ("unknow".equals(metaDataValue)) {
                isMatched = metaData == null || StringUtils.isEmpty(metaData.get(metaDataKey));
            } else {
                isMatched = metaData != null && metaDataValue.equals(metaData.get(metaDataKey));
            }
            if (isMatched) {
                results.add(server);
            }

        }
        return results;
    }


    private List<Server> chooseServerByMetaData(String metaDataKey, List<String> metaDataValues, List<Server> servers) {


        if (StringUtils.isEmpty(metaDataKey) || EmptyUtils.isEmpty(metaDataValues)) {
            return new ArrayList<>();
        }
        List<Server> results = new ArrayList<>();


        if (IP_ADDR.equals(metaDataKey)) {
            for (Server server : servers) {
                DiscoveryEnabledServer discoveryEnabledServer = (DiscoveryEnabledServer) server;
                if (metaDataValues.contains(discoveryEnabledServer.getInstanceInfo().getIPAddr())) {
                    results.add(server);
                }
            }
            return results;
        }


        Iterator<Server> serverIterator = servers.iterator();
        boolean containUnKnow = CollectionUtils.contains(metaDataValues.iterator(), "unknow");
        while (serverIterator.hasNext()) {
            DiscoveryEnabledServer server = (DiscoveryEnabledServer) serverIterator.next();
            Map<String, String> metaData = server.getInstanceInfo().getMetadata();

            boolean isMatched = false;
            if (metaData != null) {
                if (EmptyUtils.isEmpty(metaData.get(metaDataKey))) {
                    isMatched = containUnKnow;
                } else {
                    isMatched = CollectionUtils.contains(metaDataValues.iterator(), metaData.get(metaDataKey));
                }

            } else {
                if (containUnKnow) {
                    isMatched = true;
                }
            }

            if (isMatched) {
                results.add(server);
            }

        }
        return results;
    }


    private List<Server> statusFilter(List<Server> servers, InstanceInfo.InstanceStatus status) {
        Iterator<Server> iterator = servers.iterator();
        while (iterator.hasNext()) {
            DiscoveryEnabledServer discoveryEnabledServer = (DiscoveryEnabledServer) iterator.next();
            if (!discoveryEnabledServer.getInstanceInfo().getStatus().name().equals(status.name())) {
                iterator.remove();
            }
        }
        return servers;
    }


}
