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

import com.hupu.msv.apm.agent.core.context.CarrierItem;
import com.hupu.msv.apm.agent.core.context.ContextCarrier;
import com.hupu.msv.apm.agent.core.context.ContextManager;
import com.hupu.msv.apm.agent.core.context.ContextSnapshot;
import com.hupu.msv.apm.agent.core.context.tag.Tags;
import com.hupu.msv.apm.agent.core.context.trace.AbstractSpan;
import com.hupu.msv.apm.agent.core.context.trace.SpanLayer;
import com.hupu.msv.apm.agent.core.logging.api.ILog;
import com.hupu.msv.apm.agent.core.logging.api.LogManager;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.EnhancedInstance;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.InstanceMethodsAroundInterceptor;
import com.hupu.msv.apm.agent.core.plugin.interceptor.enhance.MethodInterceptResult;
import com.hupu.msv.apm.network.trace.component.ComponentsDefine;
import com.hupu.msv.apm.plugin.kafka.util.KafkaClassUtil;
import org.apache.kafka.clients.ApiVersions;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.ProducerRecord;

import java.lang.reflect.Method;

/**
 * @author zhang xin, stalary
 */
public class KafkaProducerInterceptor implements InstanceMethodsAroundInterceptor {

    private static final ILog logger = LogManager.getLogger(KafkaProducerInterceptor.class);

    public static final String OPERATE_NAME_PREFIX = "Kafka/";
    public static final String PRODUCER_OPERATE_NAME_SUFFIX = "/Producer";



    @Override
    public void beforeMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                             MethodInterceptResult result) throws Throwable {

        ContextCarrier contextCarrier = new ContextCarrier();

        ProducerRecord record = (ProducerRecord) allArguments[0];
        String topicName = record.topic();
        AbstractSpan activeSpan = ContextManager.createExitSpan(OPERATE_NAME_PREFIX + topicName + PRODUCER_OPERATE_NAME_SUFFIX, contextCarrier, (String) objInst.getSkyWalkingDynamicField());

        Tags.MQ_BROKER.set(activeSpan, (String) objInst.getSkyWalkingDynamicField());
        Tags.MQ_TOPIC.set(activeSpan, topicName);
        SpanLayer.asMQ(activeSpan);
        activeSpan.setComponent(ComponentsDefine.KAFKA_PRODUCER);

        //确认下当前服务端的消息版本
        if(KafkaClassUtil.MAX_USABLE_PRODUCE_MAGIC == -1){
            synchronized(KafkaProducerInterceptor.class) {
                long maxBlockTimeMs = (Long)KafkaClassUtil.getObjectField(objInst,"maxBlockTimeMs");

                Method waitOnMetadata = objInst.getClass().getDeclaredMethod("waitOnMetadata",String.class,Integer.class,long.class);
                waitOnMetadata.setAccessible(true);
                waitOnMetadata.invoke(objInst,record.topic(),record.partition(),maxBlockTimeMs);

                ApiVersions apiVersions=(ApiVersions)KafkaClassUtil.getObjectField(objInst,"apiVersions");
                KafkaClassUtil.MAX_USABLE_PRODUCE_MAGIC = apiVersions.maxUsableProduceMagic();
                logger.info("apiVersions.MAX_USABLE_PRODUCE_MAGIC():{}",apiVersions.maxUsableProduceMagic());
            }
        }

        //版本>=2时，可以往头部加信息。否则会导致发消息失败的问题
        if(KafkaClassUtil.MAX_USABLE_PRODUCE_MAGIC >=2){
            CarrierItem next = contextCarrier.items();
            while (next.hasNext()) {
                next = next.next();
                record.headers().add(next.getHeadKey(), next.getHeadValue().getBytes());
            }
        }

        Object shouldCallbackInstance = allArguments[1];
        if (null != shouldCallbackInstance) {
            if (shouldCallbackInstance instanceof EnhancedInstance) {
                EnhancedInstance callbackInstance = (EnhancedInstance) shouldCallbackInstance;
                ContextSnapshot snapshot = ContextManager.capture();
                if (null != snapshot) {
                    CallbackCache cache = new CallbackCache();
                    cache.setSnapshot(snapshot);
                    callbackInstance.setSkyWalkingDynamicField(cache);
                }
            } else if (shouldCallbackInstance instanceof Callback) {
                Callback callback = (Callback) shouldCallbackInstance;
                ContextSnapshot snapshot = ContextManager.capture();
                if (null != snapshot) {
                    CallbackCache cache = new CallbackCache();
                    cache.setSnapshot(snapshot);
                    cache.setCallback(callback);
                    allArguments[1] = new CallbackAdapterInterceptor(cache);
                }
            }
        }
    }

    @Override
    public Object afterMethod(EnhancedInstance objInst, Method method, Object[] allArguments, Class<?>[] argumentsTypes,
                              Object ret) throws Throwable {
        ContextManager.stopSpan();
        return ret;
    }

    @Override
    public void handleMethodException(EnhancedInstance objInst, Method method, Object[] allArguments,
                                      Class<?>[] argumentsTypes, Throwable t) {
        if (t.getMessage().equalsIgnoreCase("support record headers")) {
            KafkaClassUtil.MAX_USABLE_PRODUCE_MAGIC = 1;
        }
    }


}
