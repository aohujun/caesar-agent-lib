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

package com.hupu.msv.apm.agent.core.remote;

import io.grpc.Channel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import com.hupu.msv.apm.agent.core.boot.BootService;
import com.hupu.msv.apm.agent.core.boot.DefaultImplementor;
import com.hupu.msv.apm.agent.core.boot.DefaultNamedThreadFactory;
import com.hupu.msv.apm.agent.core.conf.Config;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.util.RunnableWithExceptionProtection;

/**
 * @author wusheng, zhang xin
 */
@DefaultImplementor
public class GRPCChannelManager implements BootService, Runnable {
    private static final ILog logger = LogManager.getLogger(GRPCChannelManager.class);

    private volatile GRPCChannel managedChannel = null;
    private volatile ScheduledFuture<?> connectCheckFuture;
    private volatile boolean reconnect = true;
    private Random random = new Random();
    private List<GRPCChannelListener> listeners = Collections.synchronizedList(new LinkedList<GRPCChannelListener>());
    private volatile List<String> grpcServers;
    private volatile int selectedIdx = -1;
    private volatile int reconnectCount = 0;

    @Override
    public void prepare() throws Throwable {

    }

    @Override
    public void boot() throws Throwable {
        if (Config.Collector.BACKEND_SERVICE.trim().length() == 0) {
            logger.error("Collector server addresses are not set.");
            logger.error("Agent will not uplink any data.");
            return;
        }
        grpcServers = Arrays.asList(Config.Collector.BACKEND_SERVICE.split(","));
        connectCheckFuture = Executors
            .newSingleThreadScheduledExecutor(new DefaultNamedThreadFactory("GRPCChannelManager"))
            .scheduleAtFixedRate(new RunnableWithExceptionProtection(this, new RunnableWithExceptionProtection.CallbackWhenException() {
                @Override
                public void handle(Throwable t) {
                    logger.error("unexpected exception.", t);
                }
            }), 0, Config.Collector.GRPC_CHANNEL_CHECK_INTERVAL, TimeUnit.SECONDS);
    }

    @Override
    public void onComplete() throws Throwable {

    }

    @Override
    public void shutdown() throws Throwable {
        if (connectCheckFuture != null) {
            connectCheckFuture.cancel(true);
        }
        if (managedChannel != null) {
            managedChannel.shutdownNow();
        }
        logger.warn("Selected collector grpc service shutdown.");
    }

    @Override
    public void run() {
        logger.debug("Selected collector grpc service running, reconnect:{}.", reconnect);
        if (reconnect) {
            if (grpcServers.size() > 0) {
                String server = "";
                try {
                    int index = Math.abs(random.nextInt()) % grpcServers.size();
                    if (index != selectedIdx) {
                        selectedIdx = index;

                        server = grpcServers.get(index);
                        String[] ipAndPort = server.split(":");

                        if (managedChannel != null) {
                            managedChannel.shutdownNow();
                        }

                        managedChannel = GRPCChannel.newBuilder(ipAndPort[0], Integer.parseInt(ipAndPort[1]))
                            .addManagedChannelBuilder(new StandardChannelBuilder())
                            .addManagedChannelBuilder(new TLSChannelBuilder())
                            .addChannelDecorator(new AgentIDDecorator())
                            .addChannelDecorator(new AuthenticationDecorator())
                            .build();
                        notify(GRPCChannelStatus.CONNECTED);
                        reconnectCount = 0;
                        reconnect = false;
                    } else if (managedChannel.isConnected(++reconnectCount > Config.Agent.FORCE_RECONNECTION_PERIOD)) {
                        // Reconnect to the same server is automatically done by GRPC,
                        // therefore we are responsible to check the connectivity and
                        // set the state and notify listeners
                        reconnectCount = 0;
                        notify(GRPCChannelStatus.CONNECTED);
                        reconnect = false;
                    }

                    return;
                } catch (Throwable t) {
                    logger.error(t, "Create channel to {} fail.", server);
                }
            }

            logger.debug("Selected collector grpc service is not available. Wait {} seconds to retry", Config.Collector.GRPC_CHANNEL_CHECK_INTERVAL);
        }
    }

    public void addChannelListener(GRPCChannelListener listener) {
        listeners.add(listener);
    }

    public Channel getChannel() {
        return managedChannel.getChannel();
    }

    /**
     * If the given expcetion is triggered by network problem, connect in background.
     *
     * @param throwable
     */
    public void reportError(Throwable throwable) {
        if (isNetworkError(throwable)) {
            reconnect = true;
            notify(GRPCChannelStatus.DISCONNECT);
        }
    }

    private void notify(GRPCChannelStatus status) {
        for (GRPCChannelListener listener : listeners) {
            try {
                listener.statusChanged(status);
            } catch (Throwable t) {
                logger.error(t, "Fail to notify {} about channel connected.", listener.getClass().getName());
            }
        }
    }

    private boolean isNetworkError(Throwable throwable) {
        if (throwable instanceof StatusRuntimeException) {
            StatusRuntimeException statusRuntimeException = (StatusRuntimeException) throwable;
            return statusEquals(statusRuntimeException.getStatus(),
                Status.UNAVAILABLE,
                Status.PERMISSION_DENIED,
                Status.UNAUTHENTICATED,
                Status.RESOURCE_EXHAUSTED,
                Status.UNKNOWN
            );
        }
        return false;
    }

    private boolean statusEquals(Status sourceStatus, Status... potentialStatus) {
        for (Status status : potentialStatus) {
            if (sourceStatus.getCode() == status.getCode()) {
                return true;
            }
        }
        return false;
    }
}
