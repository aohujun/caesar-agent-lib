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

import com.hupu.msv.apm.agent.core.context.util.PeerFormat;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.plugin.redisson.v3.util.ClassUtil;
import com.hupu.msv.apm.util.StringUtil;
import org.redisson.config.Config;
import org.redisson.connection.ConnectionManager;

import java.lang.reflect.Method;
import java.net.URI;
import java.util.Collection;

/**
 * @author zhaoyuguang
 */
public class ConnectionManagerInterceptor implements InstanceMethodsAroundInterceptor {

    private static final ILog logger = LogManager.getLogger(ConnectionManagerInterceptor.class);

    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments,
                              Class<?>[] argumentsTypes, Object ret) throws Throwable {
        try {
            EnhancedInstance retInst = (EnhancedInstance) ret;
            ConnectionManager connectionManager = (ConnectionManager) objInst;

//            if ( !haveCfgMethod(connectionManager.getClass())) {
//                //如果是老版本 那么直接赋值
//                String peerOld = (String) objInst.getSkyWalkingDynamicField();
//                if (!StringUtil.isEmpty(peerOld)) {
//                    return ret;
//                }
//                 StringBuilder peer = new StringBuilder();
//                if (argumentsTypes[0] == String.class) {
//                    if (allArguments[1] == null && StringUtil.isEmpty(allArguments[1].toString())){
//                        peer.append(allArguments[0]);
//                    }else {
//                        peer.append(allArguments[0] +":" + allArguments[1]);
//                    }
//                    retInst.setSkyWalkingDynamicField(PeerFormat.shorten(peer.toString()));
//                    return ret;
//                }
//                if (argumentsTypes[0] == org.redisson.api.NodeType.class) {
//                    if (allArguments[2] == null && StringUtil.isEmpty(allArguments[2].toString())){
//                        peer.append(allArguments[1]);
//                    }else {
//                        peer.append(allArguments[1] +":" + allArguments[2]);
//                    }
//                    retInst.setSkyWalkingDynamicField(PeerFormat.shorten(peer.toString()));
//                    return ret;
//                }
//                retInst.setSkyWalkingDynamicField(PeerFormat.shorten("unknow-redis.addrees"));
//                return ret;
//            }


            Config config = connectionManager.getCfg();
            Object singleServerConfig = ClassUtil.getObjectField(config, "singleServerConfig");
            Object sentinelServersConfig = ClassUtil.getObjectField(config, "sentinelServersConfig");
            Object masterSlaveServersConfig = ClassUtil.getObjectField(config, "masterSlaveServersConfig");
            Object clusterServersConfig = ClassUtil.getObjectField(config, "clusterServersConfig");
            Object replicatedServersConfig = ClassUtil.getObjectField(config, "replicatedServersConfig");

            StringBuilder peer = new StringBuilder();

            if (singleServerConfig != null) {
                Object singleAddress = ClassUtil.getObjectField(singleServerConfig, "address");
                peer.append(getPeer(singleAddress));
                retInst.setSkyWalkingDynamicField(PeerFormat.shorten(peer.toString()));
                return ret;
            }
            if (sentinelServersConfig != null) {
                appendAddresses(peer, (Collection) ClassUtil.getObjectField(sentinelServersConfig, "sentinelAddresses"));
                retInst.setSkyWalkingDynamicField(PeerFormat.shorten(peer.toString()));
                return ret;
            }
            if (masterSlaveServersConfig != null) {
                Object masterAddress = ClassUtil.getObjectField(masterSlaveServersConfig, "masterAddress");
                peer.append(getPeer(masterAddress));
                appendAddresses(peer, (Collection) ClassUtil.getObjectField(masterSlaveServersConfig, "slaveAddresses"));
                retInst.setSkyWalkingDynamicField(PeerFormat.shorten(peer.toString()));
                return ret;
            }
            if (clusterServersConfig != null) {
                appendAddresses(peer, (Collection) ClassUtil.getObjectField(clusterServersConfig, "nodeAddresses"));
                retInst.setSkyWalkingDynamicField(PeerFormat.shorten(peer.toString()));
                return ret;
            }
            if (replicatedServersConfig != null) {
                appendAddresses(peer, (Collection) ClassUtil.getObjectField(replicatedServersConfig, "nodeAddresses"));
                retInst.setSkyWalkingDynamicField(PeerFormat.shorten(peer.toString()));
                return ret;
            }
        } catch (Exception e) {
            logger.warn("redisClient set peer error: ", e);
        }
        return ret;
    }

    private void appendAddresses(StringBuilder peer, Collection nodeAddresses) {
        if (nodeAddresses != null && !nodeAddresses.isEmpty()) {
            for (Object uri : nodeAddresses) {
                peer.append(getPeer(uri)).append(";");
            }
        }
    }

    /**
     * In some high versions of redisson, such as 3.11.1. The attribute address in the RedisClientConfig class is
     * changed from the lower version of the URI to the String. So use the following code for compatibility.
     *
     * @param obj Address object
     * @return the sw peer
     */
    static String getPeer(Object obj) {
        if (obj instanceof String) {
            return ((String) obj).replace("redis://", "");
        } else if (obj instanceof URI) {
            URI uri = (URI) obj;
            return uri.getHost() + ":" + uri.getPort();
        } else {
            logger.warn("redisson not support this version");
            return null;
        }
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
    }

    private Boolean haveCfgMethod(Class clazz) {
        Method[]  methods = clazz.getMethods();
        if (methods == null || methods.length == 0) {
            return Boolean.FALSE;
        }
        for(Method method :methods) {
            if (method.getName().equals("getCfg")) {
                return Boolean.TRUE;
            }
        }
        return Boolean.FALSE;
    }
}
