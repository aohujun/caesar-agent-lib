/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 */

package com.hupu.msv.apm.agent.core.conf;

import com.hupu.msv.apm.agent.core.governance.gray.common.GrayConstant;

public class RuntimeContextConfiguration {

    public static String[] NEED_PROPAGATE_CONTEXT_KEY = new String[]{
            "SW_REQUEST",
            "SW_RESPONSE",
            "SW_WEBFLUX_REQUEST_KEY",
            "gray-rule",
            GrayConstant.HTTP_HEADERS_CHECKED,
            GrayConstant.EXTENSION_HTTP_HEADERS,
            GrayConstant.FSTRESS_TASK,
            GrayConstant.HTTP_REQUEST
    };
}
