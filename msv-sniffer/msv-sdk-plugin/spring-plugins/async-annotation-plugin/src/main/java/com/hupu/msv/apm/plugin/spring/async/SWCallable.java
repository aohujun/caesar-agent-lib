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
package com.hupu.msv.apm.plugin.spring.async;

import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.ContextSnapshot;
import com.hupu.msv.apm.agent.core.context.trace.AbstractSpan;
import com.hupu.msv.apm.agent.core.util.CaesarAgentUtil;
import com.hupu.msv.apm.network.trace.component.ComponentsDefine;

import java.util.concurrent.Callable;

/**
 * @author zhaoyuguang
 */
public class SWCallable<V> implements Callable<V> {

    private static final String OPERATION_NAME = "SpringAsync";

    private Callable<V> callable;

    private ContextSnapshot snapshot;

    SWCallable(Callable<V> callable, ContextSnapshot snapshot) {
        this.callable = callable;
        this.snapshot = snapshot;
    }

    @Override
    public V call() throws Exception {
        try {
            AbstractSpan span;
            if (snapshot != null && snapshot.isValid() && snapshot.getDistributedTraceId() != null) {
                String traceId = snapshot.getDistributedTraceId().toString();
                span = ContextManager.createLocalSpan(SWCallable.OPERATION_NAME, CaesarAgentUtil.isSampled(traceId));
            } else {
                span = ContextManager.createLocalSpan(SWCallable.OPERATION_NAME);
            }
            span.setComponent(ComponentsDefine.SPRING_ASYNC);
            if (snapshot != null) {
                ContextManager.continued(snapshot);
            }
            return callable.call();
        } catch (Exception e) {
            ContextManager.activeSpan().errorOccurred().log(e);
            throw e;
        } finally {
            ContextManager.stopSpan();
        }
    }
}
