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

package com.hupu.msv.apm.plugin.rocketMQ.v3;

import com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyStatus;
import java.lang.reflect.Method;
import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.tag.Tags;
import com.hupu.msv.apm.agent.core.context.trace.AbstractSpan;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;

/**
 * {@link MessageConcurrentlyConsumeInterceptor} set the process status after the {@link
 * com.alibaba.rocketmq.client.consumer.listener.MessageListenerConcurrently#consumeMessage(java.util.List,
 * com.alibaba.rocketmq.client.consumer.listener.ConsumeConcurrentlyContext)} method execute.
 *
 * @author carlvine500
 */
public class MessageConcurrentlyConsumeInterceptor extends AbstractMessageConsumeInterceptor {

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
        Object ret) throws Throwable {
        ConsumeConcurrentlyStatus status = (ConsumeConcurrentlyStatus)ret;
        if (status == ConsumeConcurrentlyStatus.RECONSUME_LATER) {
            AbstractSpan activeSpan = ContextManager.activeSpan();
            activeSpan.errorOccurred();
            Tags.STATUS_CODE.set(activeSpan, status.name());
        }
        ContextManager.stopSpan();
        return ret;
    }
}

