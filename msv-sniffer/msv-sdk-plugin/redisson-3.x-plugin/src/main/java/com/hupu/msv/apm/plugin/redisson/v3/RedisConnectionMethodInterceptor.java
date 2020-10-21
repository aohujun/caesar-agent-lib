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


package com.hupu.msv.apm.plugin.redisson.v3;

import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.plugin.redisson.v3.util.ClassUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.tag.Tags;
import com.hupu.msv.apm.agent.core.context.trace.AbstractSpan;
import com.hupu.msv.apm.agent.core.context.trace.SpanLayer;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceConstructorInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.network.trace.component.ComponentsDefine;
import org.redisson.client.RedisClient;
import org.redisson.client.RedisConnection;
import org.redisson.client.protocol.CommandData;
import org.redisson.client.protocol.CommandsData;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

/**
 * @author zhaoyuguang
 */
public class RedisConnectionMethodInterceptor implements InstanceMethodsAroundInterceptor, InstanceConstructorInterceptor {

    private static final ILog logger = LogManager.getLogger(RedisConnectionMethodInterceptor.class);

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
        String peer = (String) objInst.getSkyWalkingDynamicField();

        RedisConnection connection = (RedisConnection) objInst;
        Channel channel = connection.getChannel();
        InetSocketAddress remoteAddress = (InetSocketAddress) channel.remoteAddress();
        String dbInstance = remoteAddress.getAddress().getHostAddress() + ":" + remoteAddress.getPort();

        StringBuilder dbStatement = new StringBuilder();
        String operationName = "Redisson/";

        if (allArguments[0] instanceof CommandsData) {
            operationName = operationName + "BATCH_EXECUTE";
            CommandsData commands = (CommandsData) allArguments[0];
            for (CommandData commandData : commands.getCommands()) {
                addCommandData(dbStatement, commandData);
                dbStatement.append(";");
            }
        } else if (allArguments[0] instanceof CommandData) {
            CommandData commandData = (CommandData) allArguments[0];
            String command = commandData.getCommand().getName();
            operationName = operationName + command;
            if (ignoreCommand(command,commandData.getCommand().getSubName())) {
                return ;
            }
            addCommandData(dbStatement, commandData);
        }

        AbstractSpan span = ContextManager.createExitSpan(operationName, peer);
        span.setComponent(ComponentsDefine.REDISSON);
        Tags.DB_TYPE.set(span, "Redis");
        Tags.DB_INSTANCE.set(span, dbInstance);
        Tags.DB_STATEMENT.set(span, dbStatement.toString());
        SpanLayer.asCache(span);
    }

    private void addCommandData(StringBuilder dbStatement, CommandData commandData) {
        dbStatement.append(commandData.getCommand().getName());
        if (commandData.getParams() != null) {
            for (Object param : commandData.getParams()) {
                dbStatement.append(" ").append(param instanceof ByteBuf ? "?" : String.valueOf(param.toString()));
            }
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        if (allArguments[0] instanceof CommandData) {
            CommandData commandData = (CommandData) allArguments[0];
            String command = commandData.getCommand().getName();
            if (ignoreCommand(command,commandData.getCommand().getSubName())) {
                return ret;
            }
        }
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        if (allArguments[0] instanceof CommandData) {
            CommandData commandData = (CommandData) allArguments[0];
            String command = commandData.getCommand().getName();
            if (ignoreCommand(command,commandData.getCommand().getSubName())) {
                return ;
            }
        }
        AbstractSpan span = ContextManager.activeSpan();
        span.errorOccurred();
        span.log(t);
    }

    @Override
    public void onConstruct(EnhancedInstance objInst, Object[] allArguments) {
        String peer = (String) ((EnhancedInstance) allArguments[0]).getSkyWalkingDynamicField();
        if (peer == null) {
            try {
                /*
                  In some high versions of redisson, such as 3.11.1.
                  The attribute address in the RedisClientConfig class changed from a lower version of the URI to a RedisURI.
                  But they all have the host and port attributes, so use the following code for compatibility.
                 */
                Object address = ClassUtil.getObjectField(((RedisClient) allArguments[0]).getConfig(), "address");
                String host = (String) ClassUtil.getObjectField(address, "host");
                String port = String.valueOf(ClassUtil.getObjectField(address, "port"));
                peer = host + ":" + port;
            } catch (Exception e) {
                logger.warn("RedisConnection create peer error: ", e);
            }
        }
        objInst.setSkyWalkingDynamicField(peer);
    }

    private Boolean ignoreCommand(String fristCommand,String secondCommand) {
        String[] EXCLUDE_ENDPOINT_NAMES = new String[]{"INFO", "NODES"};
        if("READONLY".equalsIgnoreCase(fristCommand)){
            return Boolean.TRUE;
        }

        if ("CLUSTER".equalsIgnoreCase(fristCommand) ) {
            for (String endpointName : EXCLUDE_ENDPOINT_NAMES) {
                if (endpointName.equalsIgnoreCase(secondCommand)) {
                    return Boolean.TRUE;
                }
            }
        }

        return Boolean.FALSE;
    }
}
