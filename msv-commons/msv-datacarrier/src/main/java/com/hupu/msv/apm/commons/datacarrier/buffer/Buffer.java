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

package com.hupu.msv.apm.commons.datacarrier.buffer;

import com.hupu.msv.apm.commons.datacarrier.callback.QueueBlockingCallback;
import com.hupu.msv.apm.commons.datacarrier.common.AtomicRangeInteger;

import java.util.LinkedList;
import java.util.List;

/**
 * Created by wusheng on 2016/10/25.
 */
public class Buffer<T> {
    private final Object[] buffer;
    private com.hupu.msv.apm.commons.datacarrier.buffer.BufferStrategy strategy;
    private AtomicRangeInteger index;
    private List<QueueBlockingCallback<T>> callbacks;

    Buffer(int bufferSize, com.hupu.msv.apm.commons.datacarrier.buffer.BufferStrategy strategy) {
        buffer = new Object[bufferSize];
        this.strategy = strategy;
        index = new AtomicRangeInteger(0, bufferSize);
        callbacks = new LinkedList<QueueBlockingCallback<T>>();
    }

    void setStrategy(com.hupu.msv.apm.commons.datacarrier.buffer.BufferStrategy strategy) {
        this.strategy = strategy;
    }

    void addCallback(QueueBlockingCallback<T> callback) {
        callbacks.add(callback);
    }


    /**
     *   保存数据到队列的buffer里.
     *   首先获取当前buffer自增的下标，如果当前buffer对应桶数据不存在，将数据放入buffer里。
     *   如果有分三种情况：
     *     1） BLOCKING： 一直堵塞，直到当前桶被消费完，再存放数据。 这里预留了一个回调函数QueueBlockingCallback。 默认策略
     *     2） IF_POSSIBLE：如果当前桶有数据，则不存放。 TraceSegment采用这种方式生产
     *     3） OVERRIDE：覆盖当前桶的数据 。 这种方式目前没有使用。
     *
     * @param data
     * @return
     */
    boolean save(T data) {
        int i = index.getAndIncrement();
        if (buffer[i] != null) {
            switch (strategy) {
                case BLOCKING:
                    boolean isFirstTimeBlocking = true;
                    while (buffer[i] != null) {
                        if (isFirstTimeBlocking) {
                            isFirstTimeBlocking = false;
                            for (QueueBlockingCallback<T> callback : callbacks) {
                                callback.notify(data);
                            }
                        }
                        try {
                            Thread.sleep(1L);
                        } catch (InterruptedException e) {
                        }
                    }
                    break;
                case IF_POSSIBLE:
                    return false;
                case OVERRIDE:
                default:
            }
        }
        buffer[i] = data;
        return true;
    }

    public int getBufferSize() {
        return buffer.length;
    }

    public void obtain(List<T> consumeList) {
        this.obtain(consumeList, 0, buffer.length);
    }

    public void obtain(List<T> consumeList, int start, int end) {
        for (int i = start; i < end; i++) {
            if (buffer[i] != null) {
                consumeList.add((T)buffer[i]);
                buffer[i] = null;
            }
        }
    }

}
