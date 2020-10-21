package com.hupu.msv.apm.agent.core.governance;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.googlecode.protobuf.format.JsonFormat;
import com.hupu.msv.apm.agent.core.boot.*;
import com.hupu.msv.apm.agent.core.conf.Config;
import com.hupu.msv.apm.agent.core.governance.circuitbreaker.strategy.CircuitBreakerStrategyManager;
import com.hupu.msv.apm.agent.core.governance.gray.common.GrayRouteManager;
import com.hupu.msv.apm.agent.core.governance.loadbalancer.common.LoadBalancerManager;
import com.hupu.msv.apm.agent.core.governance.traffic.common.TrafficManager;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.remote.GRPCChannelListener;
import com.hupu.msv.apm.agent.core.remote.GRPCChannelManager;
import com.hupu.msv.apm.agent.core.remote.GRPCChannelStatus;
import com.hupu.msv.apm.agent.core.util.CaesarAgentUtil;
import com.hupu.msv.apm.agent.core.util.HostNameUtil;
import com.hupu.msv.apm.network.governance.*;
import com.hupu.msv.apm.util.RunnableWithExceptionProtection;
import com.hupu.msv.apm.util.StringUtil;
import io.grpc.Channel;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static com.hupu.msv.apm.agent.core.governance.GovernanceConstants.RULE_DEL_FLAG_NORMAL;
import static com.hupu.msv.apm.agent.core.governance.GovernanceConstants.RULE_ENABLED_OPEN;


/**
 * @author: zhaoxudong
 * @date: 2019-10-29 10:33
 * @description: 服务治理配置信息拉取和上报
 */
@DefaultImplementor
public class GovernanceService implements BootService, GRPCChannelListener, Runnable {
    private static final ILog logger = LogManager.getLogger(GovernanceService.class);
    private volatile GRPCChannelStatus status = GRPCChannelStatus.DISCONNECT;
    private PullConfig pullConfig;
    private volatile ScheduledFuture<?> pullConfigFuture;
    private volatile ScheduledFuture<?> cleanConfigFuture;
    private volatile AppPluginInfoServiceGrpc.AppPluginInfoServiceBlockingStub stub;

    private static String SPECIFIED_CONFIG_PATH = "governance_config";
    private static String DEFAULT_CONFIG_FILE_NAME = "/config/governance.config";
    private static String CONFIG_FILE;

    @Override
    public void prepare() throws Throwable {
        //初始化策略配置加载器
        CircuitBreakerStrategyManager.INSTANCE.init();
        GrayRouteManager.INSTANCE.init();
        TrafficManager.INSTANCE.init();
        LoadBalancerManager.INSTANCE.init();
        ServiceManager.INSTANCE.findService(GRPCChannelManager.class).addChannelListener(this);
        pullConfig = new PullConfig();
        ServiceManager.INSTANCE.findService(GRPCChannelManager.class).addChannelListener(pullConfig);
        //加入服务治理配置监听
        ServiceLoader<ConfiguratorListener> loader = ServiceLoader.load(ConfiguratorListener.class);
        Iterator<ConfiguratorListener> iterator = loader.iterator();
        while (iterator.hasNext()) {
            ConfiguratorManager.INSTANCE.addListener(iterator.next());
        }
        // 获取配置文件地址
        String specifiedConfigPath = System.getProperties().getProperty(SPECIFIED_CONFIG_PATH);
        CONFIG_FILE = StringUtil.isEmpty(specifiedConfigPath) ?
                AgentPackagePath.getPath().getPath() + DEFAULT_CONFIG_FILE_NAME : specifiedConfigPath;
        //获取服务信息
        GovernanceConfig.APP_ID = Config.Agent.SERVICE_NAME;
        GovernanceConfig.INSTANCE_IP = HostNameUtil.getIp();
        GovernanceConfig.INSTANCE_NAME = HostNameUtil.getHostName();
    }

    @Override
    public void boot() throws Throwable {
        //1. 上报服务相关信息（appId、ip、port）和插件相关信息（插件名字、版本、配置版本信息）。上报完成后启动一个定时任务，拉取配置信息
        new Thread(new RunnableWithExceptionProtection(this, (t) -> {
            logger.error("上报服务和插件信息异常.", t);
        }), "report").start();

    }

    @Override
    public void onComplete() throws Throwable {

    }

    @Override
    public void shutdown() throws Throwable {
        pullConfigFuture.cancel(true);
        cleanConfigFuture.cancel(true);
    }

    @Override
    public void statusChanged(GRPCChannelStatus status) {

        if (GRPCChannelStatus.CONNECTED.equals(status)) {
            Channel channel = ServiceManager.INSTANCE.findService(GRPCChannelManager.class).getChannel();
            stub = AppPluginInfoServiceGrpc.newBlockingStub(channel);
        }
        this.status = status;
    }

    @Override
    public void run() {
        boolean loadLocalConfig = false;
        while (true) {
            try {
                /**
                 * k8s部署时，容器内的agent目录采用的是挂载方式，会出现一个多个容器使用同一个宿主机agent目录配置文件的问题，
                 *  后续和devops商量如何解决。
                 *  目前先把配置读写本地文件的逻辑注释掉，正常情况下不会影响整体功能。
                 *  只有在应用启动时，连接不上oap端的时候agent服务治理功能不可用
                 */
                /*if (!loadLocalConfig) {

                    //加载本地服务治理配置
                    loadLocalConfig();
                    loadLocalConfig = true;
                    //启动定时任务，整理config文件
                    cleanConfigFuture = Executors
                            .newSingleThreadScheduledExecutor(new DefaultNamedThreadFactory("CleanConfig"))
                            .scheduleAtFixedRate(new RunnableWithExceptionProtection(new CleanConfig(),
                                    (t) -> {
                                        logger.error("CleanConfig failure.", t);
                                    }), 0, 1, TimeUnit.HOURS);
                }*/
                if (GRPCChannelStatus.CONNECTED.equals(status)) {
                    //java版本和gcType
                    String javaVersion = System.getProperty("java.version");
                    String gcType = "";
                    List<GarbageCollectorMXBean> garbageCollectorMXBeans = ManagementFactory.getGarbageCollectorMXBeans();
                    for (GarbageCollectorMXBean garbageCollectorMXBean : garbageCollectorMXBeans) {
                        gcType = gcType + garbageCollectorMXBean.getName() + ",";
                    }
                    gcType = gcType.substring(0, gcType.length() - 1);
                    Gson gson = new GsonBuilder().disableHtmlEscaping().create();
                    //启动参数
                    RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
                    List<String> aList = bean.getInputArguments();
                    logger.info("启动参数：{}", gson.toJson(aList));
                    List<String> plugins = CaesarAgentUtil.getPluginsNameList();
                    logger.info("插件名字列表：{}",gson.toJson(plugins));
                    //上报服务信息和插件信息逻辑
                    List<PluginInfo> pluginInfoList = new ArrayList<>();
                    String caesarVersion = CaesarAgentUtil.getVersion(GovernanceConstants.CAESAR_DEFAULT_VERSION);
                    pluginInfoList.add(PluginInfo.newBuilder().setPluginName(GovernanceEventEnum.TRAFFIC.getName())
                            .setConfigVersion(GovernanceConfig.CURRENT_TRAFFIC_VERSION)
                                    .setPluginVersion(caesarVersion)
                            .setEnabled(GovernanceConfig.TRAFFIC_ENABLED).build());
                    pluginInfoList.add(PluginInfo.newBuilder().setPluginName(GovernanceEventEnum.GRAY.getName())
                            .setConfigVersion(GovernanceConfig.CURRENT_GRAY_VERSION)
                            .setEnabled(GovernanceConfig.GRAY_ENABLED).setPluginVersion(caesarVersion).build());
                    pluginInfoList.add(PluginInfo.newBuilder().setPluginName(GovernanceEventEnum.CIRCUITBREAKER.getName())
                            .setConfigVersion(GovernanceConfig.CURRENT_CIRCUIT_BREAKER_VERSION)
                            .setEnabled(GovernanceConfig.CIRCUIT_BREAKER_ENABLED).setPluginVersion(caesarVersion).build());
                    pluginInfoList.add(PluginInfo.newBuilder().setPluginName(GovernanceEventEnum.DEGRADE.getName())
                            .setConfigVersion(GovernanceConfig.CURRENT_DEGRADE_VERSION)
                            .setEnabled(GovernanceConfig.DEGRADE_ENABLED).setPluginVersion(caesarVersion).build());
                    pluginInfoList.add(PluginInfo.newBuilder().setPluginName(GovernanceEventEnum.LOADBALANCER.getName())
                            .setConfigVersion(GovernanceConfig.CURRENT_LOAD_BALANCER_VERSION)
                            .setEnabled(GovernanceConfig.LOAD_BALANCER_ENABLED).setPluginVersion(caesarVersion).build());
                    pluginInfoList.add(PluginInfo.newBuilder().setPluginName(GovernanceEventEnum.EUREKA_HEALTH_CHECK.getName())
                            .setConfigVersion(GovernanceConfig.CURRENT_EUREKA_HEALTH_CHECK_VERSION)
                            .setEnabled(GovernanceConfig.EUREKA_HEALTH_CHECK_ENABLED).setPluginVersion(caesarVersion).build());
                    AppPluginInfoReportRequest request = AppPluginInfoReportRequest.newBuilder()
                            .setAppId(GovernanceConfig.APP_ID).setInstanceIp(GovernanceConfig.INSTANCE_IP)
                            .setAgentVersion(caesarVersion).setInstanceName(GovernanceConfig.INSTANCE_NAME)
                            .setStartupTime(System.currentTimeMillis())
                            .addAllPluginInfo(pluginInfoList)
                            .setJavaVersion(javaVersion)
                            .setGcType(gcType)
                            // 因为这里是字符串，上报信息方便。除了启动信息，这里也可以加入其他上报信息。用$$作为分割符号
                            // 启动信息 $$ 插件列表
                            .setStartupArguments(gson.toJson(aList)+"$$"+gson.toJson(plugins))
                            .build();
                    stub.reportAppPluginInfo(request);
                    //上报之后，启动定时任务拉取配置信息
                    pullConfigFuture = Executors
                            .newSingleThreadScheduledExecutor(new DefaultNamedThreadFactory("PullConfig"))
                            .scheduleAtFixedRate(new RunnableWithExceptionProtection(pullConfig,
                                    (t) -> {
                                        logger.error("PullConfig failure.", t);
                                    }), 0, 10, TimeUnit.SECONDS);
                    return;

                }

                Thread.sleep(100);
            } catch (Exception e) {
                logger.error(e.getMessage(), e);
            }
        }
    }

    /**
     * 加载本地配置
     */
    private void loadLocalConfig() {
        StringBuilder sb = readConfigFromLocalFile();
        if (sb.length() == 0) {
            return;
        }
        String[] configs = sb.toString().split("&");
        Arrays.stream(configs).forEach(config -> {
            GovernanceConfigResponse.Builder builder = GovernanceConfigResponse.newBuilder();
            JsonFormat format = new JsonFormat();
            try {
                format.merge(new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8)), builder);
            } catch (IOException e) {
                logger.error(e.getMessage(), e);
            }
            GovernanceConfigResponse response = builder.build();
            ConfiguratorManager.INSTANCE.loadConfig(response);
            GovernanceConfig.CURRENT_GLOBAL_VERSION = response.getGlobal().getVersion();
        });
    }

    /**
     * 从本地文件读取配置
     *
     * @return
     */
    private StringBuilder readConfigFromLocalFile() {
        checkFileExists();
        StringBuilder sb = new StringBuilder();
        try {
            Files.lines(Paths.get(CONFIG_FILE), StandardCharsets.UTF_8).forEach(s -> {
                sb.append(s);
            });
        } catch (IOException e) {
            logger.error("读取配置文件失败" + e.getMessage(), e);
        }
        return sb;
    }

    /**
     * 将配置追加到本地文件
     *
     * @param response
     */
    private void appendToConfigFile(GovernanceConfigResponse response) {
        checkFileExists();
        try {
            JsonFormat jsonFormat = new JsonFormat();
            Files.write(Paths.get(CONFIG_FILE), ("&" + jsonFormat.printToString(response)).getBytes(StandardCharsets.UTF_8),
                    StandardOpenOption.APPEND);
        } catch (IOException e) {
            logger.error("配置追加失败，" + e.getMessage(), e);
        }
    }

    /**
     * 将配置追写到本地文件
     *
     * @param response
     */
    private void writeToConfigFile(GovernanceConfigResponse response) {
        checkFileExists();
        try {
            JsonFormat jsonFormat = new JsonFormat();
            Files.write(Paths.get(CONFIG_FILE), jsonFormat.printToString(response).getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            logger.error("配置追加失败，" + e.getMessage(), e);
        }
    }

    private void checkFileExists() {
        if (!new File(CONFIG_FILE).exists()) {
            File file = new File(CONFIG_FILE);
            try {
                file.createNewFile();
            } catch (IOException e) {
                logger.error("创建文件失败，" + e.getMessage(), e);
            }
        }
    }


    private class PullConfig implements Runnable, GRPCChannelListener {
        private volatile GRPCChannelStatus status = GRPCChannelStatus.DISCONNECT;
        private volatile GovernanceConfigServiceGrpc.GovernanceConfigServiceBlockingStub stub = null;

        @Override
        public void run() {
            if (GRPCChannelStatus.CONNECTED.equals(status)) {
                //1. 增量拉取配置
                GovernanceConfigResponse response = stub.pullConfig(GovernanceConfigRequest.newBuilder()
                        .setAppId(GovernanceConfig.APP_ID).setInstanceIp(GovernanceConfig.INSTANCE_IP).
                                setVersion(GovernanceConfig.CURRENT_GLOBAL_VERSION).build());
                if (response == null || !response.hasGlobal()) {
                    return;
                }
                //2. 将配置追加到本地文件
//                appendToConfigFile(response);
                //3. 分发配置给各服务治理组件
                ConfiguratorManager.INSTANCE.loadConfig(response);
                GovernanceConfig.CURRENT_GLOBAL_VERSION = response.getGlobal().getVersion();
                //4. 上报配置版本
                List<PluginConfigInfo> configInfoList = new ArrayList<>();
                configInfoList.add(PluginConfigInfo.newBuilder().setPluginName(GovernanceEventEnum.TRAFFIC.getName())
                        .setVersion(GovernanceConfig.CURRENT_TRAFFIC_VERSION).build());
                configInfoList.add(PluginConfigInfo.newBuilder().setPluginName(GovernanceEventEnum.GRAY.getName())
                        .setVersion(GovernanceConfig.CURRENT_GRAY_VERSION).build());
                configInfoList.add(PluginConfigInfo.newBuilder().setPluginName(GovernanceEventEnum.CIRCUITBREAKER.getName())
                        .setVersion(GovernanceConfig.CURRENT_CIRCUIT_BREAKER_VERSION).build());
                configInfoList.add(PluginConfigInfo.newBuilder().setPluginName(GovernanceEventEnum.DEGRADE.getName())
                        .setVersion(GovernanceConfig.CURRENT_DEGRADE_VERSION).build());
                configInfoList.add(PluginConfigInfo.newBuilder().setPluginName(GovernanceEventEnum.LOADBALANCER.getName())
                        .setVersion(GovernanceConfig.CURRENT_LOAD_BALANCER_VERSION).build());
                configInfoList.add(PluginConfigInfo.newBuilder().setPluginName(GovernanceEventEnum.EUREKA_HEALTH_CHECK.getName())
                        .setVersion(GovernanceConfig.CURRENT_EUREKA_HEALTH_CHECK_VERSION).build());
                ConfigVersionReportRequest reportRequest = ConfigVersionReportRequest.newBuilder()
                        .setAppId(GovernanceConfig.APP_ID).setInstanceIp(GovernanceConfig.INSTANCE_IP)
                        .addAllPluginInfo(configInfoList).build();
                stub.reportVersion(reportRequest);
            }
        }

        @Override
        public void statusChanged(GRPCChannelStatus status) {
            if (GRPCChannelStatus.CONNECTED.equals(status)) {
                Channel channel = ServiceManager.INSTANCE.findService(GRPCChannelManager.class).getChannel();
                stub = GovernanceConfigServiceGrpc.newBlockingStub(channel);
            }
            this.status = status;
        }
    }

    private class CleanConfig implements Runnable {
        Map<Long, GrayConfig> grayConfigMap = new ConcurrentHashMap<>();
        Map<Long, CircuitBreakerConfig> circuitBreakerConfigMap = new ConcurrentHashMap<>();
        Map<Long, DegradeConfig> degradeConfigMap = new ConcurrentHashMap<>();
        Map<Long, TrafficConfig> trafficConfigMap = new ConcurrentHashMap<>();
        Map<String, String> pluginEnabled = new HashMap<>();

        @Override
        public void run() {
            StringBuilder sb = readConfigFromLocalFile();
            if (sb.length() == 0) {
                return;
            }
            String[] configs = sb.toString().split("&");
            if (configs.length <= 1) {
                //只有一条配置无需整理
                return;
            }
            AtomicReference<Global> global = new AtomicReference<>();
            Arrays.stream(configs).forEach(config -> {
                GovernanceConfigResponse.Builder builder = GovernanceConfigResponse.newBuilder();
                JsonFormat format = new JsonFormat();
                try {
                    format.merge(new ByteArrayInputStream(config.getBytes(StandardCharsets.UTF_8)), builder);
                } catch (IOException e) {
                    logger.error(e.getMessage(), e);
                }
                GovernanceConfigResponse response = builder.build();
                //设置全局version
                global.set(Global.newBuilder().setVersion(response.getGlobal().getVersion()).build());
                if (response.hasGray()) {
                    cleanGrayConfig(response.getGray());
                }
                if (response.hasCircuitBreaker()) {
                    cleanCircuitBreakerConfig(response.getCircuitBreaker());
                }
                if (response.hasDegrade()) {
                    cleanDegradeConfig(response.getDegrade());
                }
                if (response.hasTraffic()) {
                    cleanTrafficConfig(response.getTraffic());
                }
            });
            Gray gray = Gray.newBuilder().setEnabled(pluginEnabled.get(GovernanceEventEnum.GRAY.getName())).addAllConfig(grayConfigMap.values()).build();
            CircuitBreaker circuitBreaker = CircuitBreaker.newBuilder().setEnabled(pluginEnabled.get(GovernanceEventEnum.CIRCUITBREAKER.getName())).addAllConfig(circuitBreakerConfigMap.values()).build();
            Degrade degrade = Degrade.newBuilder().setEnabled(pluginEnabled.get(GovernanceEventEnum.DEGRADE.getName())).addAllConfig(degradeConfigMap.values()).build();
            Traffic traffic = Traffic.newBuilder().setEnabled(pluginEnabled.get(GovernanceEventEnum.TRAFFIC.getName())).addAllConfig(trafficConfigMap.values()).build();
            //写到文件中
            writeToConfigFile(GovernanceConfigResponse.newBuilder().setGlobal(global.get())
                    .setDegrade(degrade)
                    .setGray(gray)
                    .setCircuitBreaker(circuitBreaker)
                    .setTraffic(traffic).build());
            //清空内存
            grayConfigMap.clear();
            circuitBreakerConfigMap.clear();
            degradeConfigMap.clear();
            trafficConfigMap.clear();
            pluginEnabled.clear();
        }

        private void cleanTrafficConfig(Traffic traffic) {
            if ("false".equalsIgnoreCase(traffic.getEnabled()) || "true".equalsIgnoreCase(traffic.getEnabled())) {
                pluginEnabled.put(GovernanceEventEnum.TRAFFIC.getName(), traffic.getEnabled());
            }
            traffic.getConfigList().stream().forEach(config -> {
                if (config.getDelFlag() == RULE_DEL_FLAG_NORMAL && config.getEnabled() == RULE_ENABLED_OPEN) {
                    trafficConfigMap.put(config.getId(), config);
                } else {
                    trafficConfigMap.remove(config.getId());
                }
            });
        }

        private void cleanDegradeConfig(Degrade degrade) {
            if ("false".equalsIgnoreCase(degrade.getEnabled()) || "true".equalsIgnoreCase(degrade.getEnabled())) {
                pluginEnabled.put(GovernanceEventEnum.DEGRADE.getName(), degrade.getEnabled());
            }
            degrade.getConfigList().stream().forEach(config -> {
                if (config.getDelFlag() == RULE_DEL_FLAG_NORMAL && config.getEnabled() == RULE_ENABLED_OPEN) {
                    degradeConfigMap.put(config.getId(), config);
                } else {
                    degradeConfigMap.remove(config.getId());
                }
            });
        }

        private void cleanCircuitBreakerConfig(CircuitBreaker circuitBreaker) {
            if ("false".equalsIgnoreCase(circuitBreaker.getEnabled()) || "true".equalsIgnoreCase(circuitBreaker.getEnabled())) {
                pluginEnabled.put(GovernanceEventEnum.CIRCUITBREAKER.getName(), circuitBreaker.getEnabled());
            }

            circuitBreaker.getConfigList().stream().forEach(config -> {
                if (config.getDelFlag() == RULE_DEL_FLAG_NORMAL && config.getEnabled() == RULE_ENABLED_OPEN) {
                    circuitBreakerConfigMap.put(config.getId(), config);
                } else {
                    circuitBreakerConfigMap.remove(config.getId());
                }
            });
        }

        private void cleanGrayConfig(Gray gray) {
            if ("false".equalsIgnoreCase(gray.getEnabled()) || "true".equalsIgnoreCase(gray.getEnabled())) {
                pluginEnabled.put(GovernanceEventEnum.GRAY.getName(), gray.getEnabled());
            }
            gray.getConfigList().stream().forEach(config -> {
                if (config.getDelFlag() == RULE_DEL_FLAG_NORMAL && config.getEnabled() == RULE_ENABLED_OPEN) {
                    grayConfigMap.put(config.getId(), config);
                } else {
                    grayConfigMap.remove(config.getId());
                }
            });
        }
    }
}
