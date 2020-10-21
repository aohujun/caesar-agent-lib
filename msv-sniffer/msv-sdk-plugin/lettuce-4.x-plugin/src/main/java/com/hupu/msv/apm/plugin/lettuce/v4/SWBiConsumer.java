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

package com.hupu.msv.apm.plugin.lettuce.v4;

import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.ContextSnapshot;
import com.hupu.msv.apm.agent.core.context.trace.AbstractSpan;
import com.hupu.msv.apm.agent.core.util.CaesarAgentUtil;
import com.hupu.msv.apm.network.trace.component.ComponentsDefine;

import java.util.function.BiConsumer;

public class SWBiConsumer<T, U> implements BiConsumer<T, U> {

    private BiConsumer<T, U> biConsumer;
    private ContextSnapshot snapshot;
    private String operationName;

    SWBiConsumer(BiConsumer<T, U> biConsumer, ContextSnapshot snapshot, String operationName) {
        this.biConsumer = biConsumer;
        this.snapshot = snapshot;
        this.operationName = operationName;
    }

    @Override
    public void accept(T t, U u) {
        String traceId = null;
        if (snapshot != null && snapshot.isValid() && snapshot.getDistributedTraceId() != null) {
            traceId = snapshot.getDistributedTraceId().toString();
        }
        AbstractSpan span = ContextManager.createLocalSpan(operationName + "/accept", CaesarAgentUtil.isSampled(traceId));
        span.setComponent(ComponentsDefine.LETTUCE);
        try {
            ContextManager.continued(snapshot);
            biConsumer.accept(t, u);
        } catch (Throwable th) {
            ContextManager.activeSpan().errorOccurred().log(th);
        } finally {
            ContextManager.stopSpan();
        }
    }
}
