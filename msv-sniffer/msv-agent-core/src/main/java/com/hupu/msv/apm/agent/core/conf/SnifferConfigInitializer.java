/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.hupu.msv.apm.agent.core.conf;

import com.hupu.msv.apm.agent.core.boot.AgentPackageNotFoundException;
import com.hupu.msv.apm.agent.core.boot.AgentPackagePath;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.util.ConfigInitializer;
import com.hupu.msv.apm.util.PropertyPlaceholderHelper;
import com.hupu.msv.apm.util.StringUtil;

import java.io.*;
import java.util.*;

/**
 * The <code>SnifferConfigInitializer</code> initializes all configs in several way.
 *
 * @author wusheng
 */
public class SnifferConfigInitializer {
    private static final ILog logger = LogManager.getLogger(SnifferConfigInitializer.class);
    private static String SPECIFIED_CONFIG_PATH = "agent_config";
    private static String DEFAULT_CONFIG_FILE_NAME = "/config/agent.config";
    private static String ENV_KEY_PREFIX = "msv.";
    private static boolean IS_INIT_COMPLETED = false;

    /**
     * If the specified agent config path is set, the agent will try to locate the specified agent config. If the
     * specified agent config path is not set , the agent will try to locate `agent.config`, which should be in the
     * /config directory of agent package.
     * <p>
     * Also try to override the config by system.properties. All the keys in this place should
     * start with {@link #ENV_KEY_PREFIX}. e.g. in env `msv.agent.service_name=yourAppName` to override
     * `agent.service_name` in config file.
     * <p>
     * At the end, `agent.service_name` and `collector.servers` must not be blank.
     */
    public static void initialize(String agentOptions) throws ConfigNotFoundException, AgentPackageNotFoundException {
        InputStreamReader configFileStream;

        try {
            configFileStream = loadConfig();
            Properties properties = new Properties();
            properties.load(configFileStream);
            for (String key : properties.stringPropertyNames()) {
                String value = (String)properties.get(key);
                //replace the key's value. properties.replace(key,value) in jdk8+
                properties.put(key, PropertyPlaceholderHelper.INSTANCE.replacePlaceholders(value, properties));
            }
            ConfigInitializer.initialize(properties, Config.class);
        } catch (Exception e) {
            logger.error(e, "Failed to read the config file, skywalking is going to run in default config.");
        }

        try {
            overrideConfigBySystemProp();
        } catch (Exception e) {
            logger.error(e, "Failed to read the system properties.");
        }

        if (!StringUtil.isEmpty(agentOptions)) {
            try {
                agentOptions = agentOptions.trim();
                logger.info("Agent options is {}.", agentOptions);

                overrideConfigByAgentOptions(agentOptions);
            } catch (Exception e) {
                logger.error(e, "Failed to parse the agent options, val is {}.", agentOptions);
            }
        }

        if(StringUtil.isEmpty(Config.Agent.DEPENDENCY_EXT_CLASS_PATH)){
            throw new ExceptionInInitializerError("`agent.dependency_ext_class_path` 没有找到");
        }

        if (StringUtil.isEmpty(Config.Agent.SERVICE_NAME)) {
            throw new ExceptionInInitializerError("`agent.service_name` is missing.");
        }
        if (StringUtil.isEmpty(Config.Collector.BACKEND_SERVICE)) {
            throw new ExceptionInInitializerError("`collector.backend_service` is missing.");
        }
        if (Config.Plugin.PEER_MAX_LENGTH <= 3) {
            logger.warn("PEER_MAX_LENGTH configuration:{} error, the default value of 200 will be used.", Config.Plugin.PEER_MAX_LENGTH);
            Config.Plugin.PEER_MAX_LENGTH = 200;
        }

        IS_INIT_COMPLETED = true;
    }

    private static void overrideConfigByAgentOptions(String agentOptions) throws IllegalAccessException {
        Properties properties = new Properties();
        for (List<String> terms : parseAgentOptions(agentOptions)) {
            if (terms.size() != 2) {
                throw new IllegalArgumentException("[" + terms + "] is not a key-value pair.");
            }
            properties.put(terms.get(0), terms.get(1));
        }
        if (!properties.isEmpty()) {
            ConfigInitializer.initialize(properties, Config.class);
        }
    }

    private static List<List<String>> parseAgentOptions(String agentOptions) {
        List<List<String>> options = new ArrayList<List<String>>();
        List<String> terms = new ArrayList<String>();
        boolean isInQuotes = false;
        StringBuilder currentTerm = new StringBuilder();
        for (char c : agentOptions.toCharArray()) {
            if (c == '\'' || c == '"') {
                isInQuotes = !isInQuotes;
            } else if (c == '=' && !isInQuotes) {   // key-value pair uses '=' as separator
                terms.add(currentTerm.toString());
                currentTerm = new StringBuilder();
            } else if (c == ',' && !isInQuotes) {   // multiple options use ',' as separator
                terms.add(currentTerm.toString());
                currentTerm = new StringBuilder();

                options.add(terms);
                terms = new ArrayList<String>();
            } else {
                currentTerm.append(c);
            }
        }
        // add the last term and option without separator
        terms.add(currentTerm.toString());
        options.add(terms);
        return options;
    }

    public static boolean isInitCompleted() {
        return IS_INIT_COMPLETED;
    }

    /**
     * Override the config by system properties. The property key must start with `msv`, the result should be as same
     * as in `agent.config`
     * <p>
     * such as: Property key of `agent.service_name` should be `msv.agent.service_name`
     *
     */
    private static void overrideConfigBySystemProp() throws IllegalAccessException {
        Properties properties = new Properties();
        Properties systemProperties = System.getProperties();
        Iterator<Map.Entry<Object, Object>> entryIterator = systemProperties.entrySet().iterator();
        while (entryIterator.hasNext()) {
            Map.Entry<Object, Object> prop = entryIterator.next();
            String key = prop.getKey().toString();
            if (key.startsWith(ENV_KEY_PREFIX)) {
                String realKey = key.substring(ENV_KEY_PREFIX.length());
                properties.put(realKey, prop.getValue());
            }
        }

        if (!properties.isEmpty()) {
            ConfigInitializer.initialize(properties, Config.class);
        }
    }

    /**
     * Load the specified config file or default config file
     *
     * @return the config file {@link InputStream}, or null if not needEnhance.
     */
    private static InputStreamReader loadConfig() throws AgentPackageNotFoundException, ConfigNotFoundException, ConfigReadFailedException {

        String specifiedConfigPath = System.getProperties().getProperty(SPECIFIED_CONFIG_PATH);
        File configFile = StringUtil.isEmpty(specifiedConfigPath) ? new File(AgentPackagePath.getPath(), DEFAULT_CONFIG_FILE_NAME) : new File(specifiedConfigPath);

        if (configFile.exists() && configFile.isFile()) {
            try {
                logger.info("Config file found in {}.", configFile);

                return new InputStreamReader(new FileInputStream(configFile), "UTF-8");
            } catch (FileNotFoundException e) {
                throw new ConfigNotFoundException("Failed to load agent.config", e);
            } catch (UnsupportedEncodingException e) {
                throw new ConfigReadFailedException("Failed to load agent.config", e);
            }
        }
        throw new ConfigNotFoundException("Failed to load agent.config.");
    }
}
