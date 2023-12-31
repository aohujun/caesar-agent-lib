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

package com.hupu.msv.apm.plugin.spring.mvc.commons.interceptor;

import org.springframework.web.bind.annotation.*;

import java.lang.reflect.Method;

/**
 * The <code>RestMappingMethodInterceptor</code> only use the first mapping value.
 * it will interceptor with
 * <code>@GetMapping</code>, <code>@PostMapping</code>, <code>@PutMapping</code>
 * <code>@DeleteMapping</code>, <code>@PatchMapping</code>
 *
 * @author clevertension
 */
public class RestMappingMethodInterceptor extends AbstractMethodInterceptor {
    @Override
    public String getRequestURL(Method method) {
        String requestURL = "";
        GetMapping getMapping = method.getAnnotation(GetMapping.class);
        PostMapping postMapping = method.getAnnotation(PostMapping.class);
        PutMapping putMapping = method.getAnnotation(PutMapping.class);
        DeleteMapping deleteMapping = method.getAnnotation(DeleteMapping.class);
        PatchMapping patchMapping = method.getAnnotation(PatchMapping.class);
        if (getMapping != null) {
            if (getMapping.value().length > 0) {
                requestURL = getMapping.value()[0];
            } else if (getMapping.path().length > 0) {
                requestURL = getMapping.path()[0];
            }
        } else if (postMapping != null) {
            if (postMapping.value().length > 0) {
                requestURL = postMapping.value()[0];
            } else if (postMapping.path().length > 0) {
                requestURL = postMapping.path()[0];
            }
        } else if (putMapping != null) {
            if (putMapping.value().length > 0) {
                requestURL = putMapping.value()[0];
            } else if (putMapping.path().length > 0) {
                requestURL = putMapping.path()[0];
            }
        } else if (deleteMapping != null) {
            if (deleteMapping.value().length > 0) {
                requestURL = deleteMapping.value()[0];
            } else if (deleteMapping.path().length > 0) {
                requestURL = deleteMapping.path()[0];
            }
        } else if (patchMapping != null) {
            if (patchMapping.value().length > 0) {
                requestURL = patchMapping.value()[0];
            } else if (patchMapping.path().length > 0) {
                requestURL = patchMapping.path()[0];
            }
        }
        return requestURL;
    }

    @Override
    public String getAcceptedMethodTypes(Method method) {
        return "";
    }
}
