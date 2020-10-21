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


package com.hupu.msv.apm.commons.datacarrier.partition;

import com.hupu.msv.apm.commons.datacarrier.buffer.BufferStrategy;


/**
 *   数据分配接口
 */

/**
 * Created by wusheng on 2016/10/25.
 */
public interface IDataPartitioner<T> {

    /**
     *  获得数据被分配的分区位置
     * @param total
     * @param data
     * @return
     */
    int partition(int total, T data);

    /**
     *
     * 获取最大重试次数
     * @return an integer represents how many times should retry when {@link BufferStrategy#IF_POSSIBLE}.
     *
     * Less or equal 1, means not support retry.
     */
    int maxRetryCount();
}
