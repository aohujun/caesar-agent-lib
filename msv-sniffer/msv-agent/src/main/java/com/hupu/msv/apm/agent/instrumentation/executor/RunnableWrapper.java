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
package com.hupu.msv.apm.agent.instrumentation.executor;

import com.hupu.msv.apm.agent.core.conf.RuntimeContextConfiguration;
import com.hupu.msv.apm.agent.core.context.*;
import com.hupu.msv.apm.agent.core.governance.gray.common.GrayConstant;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.util.CaesarAgentUtil;
import com.hupu.msv.apm.util.StringUtil;

import java.lang.reflect.Method;

public class RunnableWrapper implements Runnable {
    private static final ILog logger = LogManager.getLogger(RunnableWrapper.class);

    private Runnable runnable;

    private ContextSnapshot snapshot;

    private RuntimeContextSnapshot runtimeContextSnapshot;

    private Object obj;

    private Method originMethod;

    public RunnableWrapper(Runnable runnable, ContextSnapshot snapshot,
                           RuntimeContextSnapshot runtimeContextSnapshot,
                           Object obj,
                           Method originMethod) {
        this.runnable = runnable;
        this.snapshot = snapshot;
        this.runtimeContextSnapshot = runtimeContextSnapshot;
        this.obj = obj;
        this.originMethod = originMethod;
    }

    @Override
    public void run() {
        before();
        try {
            runnable.run();
        } catch (Throwable e) {
            handleException(e);
        } finally {
            after();
        }

    }

    private void before() {
        String traceId = null;

        if (snapshot != null && snapshot.isValid() && snapshot.getDistributedTraceId() != null) {
            traceId = snapshot.getDistributedTraceId().toString();
        }
        if (ignoreThread()) {
            ContextManager.createLocalSpan("Thread/" + obj.getClass().getName() + "/" + originMethod.getName(), false);
        } else {
            ContextManager.createLocalSpan("Thread/" + obj.getClass().getName() + "/" + originMethod.getName(), CaesarAgentUtil.isSampled(traceId));
        }
        if (snapshot != null) {
            ContextManager.continued(snapshot);
        }
        if (runtimeContextSnapshot != null) {
            ContextManager.getRuntimeContext().accept(runtimeContextSnapshot);
        }

    }

    private void after() {
        for (String key : RuntimeContextConfiguration.NEED_PROPAGATE_CONTEXT_KEY) {
            ContextManager.getRuntimeContext().remove(key);
        }
        ContextManager.stopSpan();
        snapshot = null;
        runtimeContextSnapshot = null;

    }

    private void handleException(Throwable e) {
        ContextManager.activeSpan().errorOccurred().log(e);
    }


    private Boolean ignoreThread() {
        if (obj == null) {
            return Boolean.FALSE;
        }

        String[] EXCLUDE_ENDPOINT_NAMES = new String[]{
                "lettuce-eventExecutorLoop",
                "lettuce-nioEventLoop",
                "lettuce-epollEventLoop",
                "lettuce-kqueueEventLoop",
                "redisson-netty",

        };

//        logger.warn("obj.getClass().getName() :"+ obj.getClass().getName());
        String threadName = null;
        if ("io.netty.channel.nio.NioEventLoop".equalsIgnoreCase(obj.getClass().getName())) {
            try {
                threadName = NioEventLoopUtil.getLoopName2NioEventLoop(obj);
            } catch (Exception e) {
                logger.warn("obj.getClass().getName() Error :" + e.getMessage());
            }
        }
        if ("com.aliyun.openservices.shade.io.netty.util.concurrent.DefaultEventExecutor".equalsIgnoreCase(obj.getClass().getName())
                || "com.aliyun.openservices.shade.io.netty.channel.nio.NioEventLoop".equalsIgnoreCase(obj.getClass().getName())) {
            return Boolean.TRUE;
        }
//        logger.warn("ignore thread :"+ threadName);
        if (!StringUtil.isEmpty(threadName)) {
            for (String endpointName : EXCLUDE_ENDPOINT_NAMES) {
                if (threadName.startsWith(endpointName)) {
                    return Boolean.TRUE;
                }
            }
        }
        return Boolean.FALSE;
    }
}
