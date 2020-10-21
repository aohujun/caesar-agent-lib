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

package com.hupu.msv.apm.agent.core.context;

import com.hupu.msv.apm.agent.core.boot.*;
import com.hupu.msv.apm.agent.core.conf.Config;
import com.hupu.msv.apm.agent.core.sampling.SamplingService;
import com.hupu.msv.apm.agent.core.util.CaesarAgentUtil;

/**
 * @author wusheng
 */
@DefaultImplementor
public class ContextManagerExtendService implements BootService {
    @Override
    public void prepare() {

    }

    @Override
    public void boot() {

    }

    @Override
    public void onComplete() {

    }

    @Override
    public void shutdown() {

    }

    public AbstractTracerContext createTraceContext(String operationName, boolean forceSampling, boolean caesarSampling, boolean crossThread) {
        AbstractTracerContext context;
        int suffixIdx = operationName.lastIndexOf(".");
        if (suffixIdx > -1 && Config.Agent.IGNORE_SUFFIX.contains(operationName.substring(suffixIdx))) {
            context = new IgnoredTracerContext();
        } else {
            SamplingService samplingService = ServiceManager.INSTANCE.findService(SamplingService.class);
            if (forceSampling || samplingService.trySampling()) {
                context = new TracingContext();
                if (!caesarSampling) {
                    //不采样，生成ignoreContext
                    context = new IgnoredTracerContext();
                } else if ((ContextManager.getRuntimeContext().get("cross-process") != null && ((boolean) ContextManager.getRuntimeContext().get("cross-process"))) || crossThread) {
                    //如果是跨进程或跨线程，不需要重复判断是否采样。什么也不做
                } else if (!CaesarAgentUtil.isSampled(context.getReadableGlobalTraceId())) {
                    //根据采样率判断是否需要生成context。不需要采样，所以，traceId要一致，这样计算的采样率才唯一
                    context = new IgnoredTracerContext(context.getReadableGlobalTraceId());
                }
            } else {
                context = new IgnoredTracerContext();
            }
        }

        return context;
    }
}
