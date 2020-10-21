package com.hupu.msv.apm.agent.core.util;


import com.hupu.msv.apm.agent.core.boot.AgentPackagePath;
import com.hupu.msv.apm.agent.core.conf.Config;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.util.StringUtil;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author: zhaoxudong
 * @date: 2019/4/22 17:05
 * @description: 版本工具
 */
public final class CaesarAgentUtil {
    private static final ILog logger = LogManager.getLogger(CaesarAgentUtil.class);
    private static String SPECIFIED_CONFIG_PATH = "caesar_version_info";
    private static String DEFAULT_CONFIG_FILE_NAME = "/config/version.info";
    private static String CONFIG_FILE;
    private static String PLUGINS_FOLDER_NAME = "/plugins/";

    public static String getVersion(String defaultVersion) {
        try {
            // 获取配置文件地址
            String specifiedConfigPath = System.getProperties().getProperty(SPECIFIED_CONFIG_PATH);
            CONFIG_FILE = StringUtil.isEmpty(specifiedConfigPath) ?
                    AgentPackagePath.getPath().getPath() + DEFAULT_CONFIG_FILE_NAME : specifiedConfigPath;
            StringBuilder sb = new StringBuilder();
            Files.lines(Paths.get(CONFIG_FILE), StandardCharsets.UTF_8).forEach(s -> {
                sb.append(s);
            });
            return sb.toString();
        } catch (Throwable e) {
            logger.error(e.getMessage() + "获取版本信息异常", e);
            return defaultVersion;
        }
    }

    /**
     * 获取插件名字列表
     *
     * @return
     */
    public static List<String> getPluginsNameList() {
        List<String> names = new ArrayList<>();
        try {
            Files.list(Paths.get(AgentPackagePath.getPath().getPath() + PLUGINS_FOLDER_NAME)).forEach(path ->
                    names.add(path.getFileName().toString())
            );
        } catch (Exception e) {
            logger.error("获取插件名字列表异常" + e.getMessage(), e);
        }
        return names;
    }

    /**
     * trace是否采样
     *
     * @param tranceId
     * @return
     */
    public static boolean isSampled(String tranceId) {
        if (tranceId == null || "N/A".equalsIgnoreCase(tranceId) || "[Ignored Trace]".equalsIgnoreCase(tranceId)) {
            return false;
        } else {
            return (tranceId.hashCode() & Integer.MAX_VALUE) % 100 < Config.Agent.SAMPLE_RATE;
        }
    }
}
