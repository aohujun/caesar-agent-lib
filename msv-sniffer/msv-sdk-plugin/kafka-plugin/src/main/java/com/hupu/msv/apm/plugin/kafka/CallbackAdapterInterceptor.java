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

package com.hupu.msv.apm.plugin.kafka;

import com.hupu.msv.apm.agent.core.util.CaesarAgentUtil;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.RecordMetadata;
import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.ContextSnapshot;
import com.hupu.msv.apm.agent.core.context.tag.Tags;
import com.hupu.msv.apm.agent.core.context.trace.AbstractSpan;
import com.hupu.msv.apm.network.trace.component.ComponentsDefine;

/**
 * implements Callback and EnhancedInstance, for kafka callback in lambda expression
 */
public class CallbackAdapterInterceptor implements Callback {

    /**
     * user Callback object
     */
    private CallbackCache callbackCache;

    public CallbackAdapterInterceptor(CallbackCache callbackCache) {
        this.callbackCache = callbackCache;
    }

    @Override
    public void onCompletion(RecordMetadata metadata, Exception exception) {
        ContextSnapshot snapshot = callbackCache.getSnapshot();
        String traceId = null;
        if (snapshot != null && snapshot.isValid() && snapshot.getDistributedTraceId() != null) {
            traceId = snapshot.getDistributedTraceId().toString();
        }
        AbstractSpan activeSpan = ContextManager.createLocalSpan("Kafka/Producer/Callback", CaesarAgentUtil.isSampled(traceId));
        activeSpan.setComponent(ComponentsDefine.KAFKA_PRODUCER);
        if (metadata != null) {
            Tags.MQ_TOPIC.set(activeSpan, metadata.topic());
        }
        ContextManager.continued(snapshot);

        try {
            callbackCache.getCallback().onCompletion(metadata, exception);
        } catch (Throwable t) {
            ContextManager.activeSpan().errorOccurred().log(t);
            throw new RuntimeException(t);
        } finally {
            if (exception != null) {
                ContextManager.activeSpan().errorOccurred().log(exception);
            }
            ContextManager.stopSpan();
        }
    }
}
