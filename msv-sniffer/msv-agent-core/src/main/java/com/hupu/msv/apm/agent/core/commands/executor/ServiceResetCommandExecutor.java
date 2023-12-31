/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hupu.msv.apm.agent.core.commands.executor;

import com.hupu.msv.apm.agent.core.boot.ServiceManager;
import com.hupu.msv.apm.agent.core.commands.CommandExecutionException;
import com.hupu.msv.apm.agent.core.commands.CommandExecutor;
import com.hupu.msv.apm.agent.core.conf.RemoteDownstreamConfig;
import com.hupu.msv.apm.agent.core.dictionary.DictionaryUtil;
import com.hupu.msv.apm.agent.core.dictionary.EndpointNameDictionary;
import com.hupu.msv.apm.agent.core.dictionary.NetworkAddressDictionary;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.remote.ServiceAndEndpointRegisterClient;
import com.hupu.msv.apm.network.trace.component.command.BaseCommand;
import com.hupu.msv.apm.network.trace.component.command.ServiceResetCommand;

/**
 * Command executor that executes the {@link ServiceResetCommand} command
 *
 * @author Zhang Xin
 * @author kezhenxu94
 */
public class ServiceResetCommandExecutor implements CommandExecutor {
    private static final ILog LOGGER = LogManager.getLogger(ServiceResetCommandExecutor.class);

    @Override
    public void execute(final BaseCommand command) throws CommandExecutionException {
        LOGGER.warn("Received ServiceResetCommand, a re-register task is scheduled.");

        ServiceManager.INSTANCE.findService(ServiceAndEndpointRegisterClient.class).coolDown();

        RemoteDownstreamConfig.Agent.SERVICE_ID = DictionaryUtil.nullValue();
        RemoteDownstreamConfig.Agent.SERVICE_INSTANCE_ID = DictionaryUtil.nullValue();
        RemoteDownstreamConfig.Agent.INSTANCE_REGISTERED_TIME = DictionaryUtil.nullValue();

        NetworkAddressDictionary.INSTANCE.clear();
        EndpointNameDictionary.INSTANCE.clear();
    }
}
