/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.hupu.msv.apm.agent.core.commands;

import com.hupu.msv.apm.network.trace.component.command.BaseCommand;

/**
 * Command executor that can handle a given command, implementations are required to be stateless,
 * i.e. the previous execution of a command cannot affect the next execution of another command.
 *
 * @author Zhang Xin
 * @author kezhenxu94
 */
public interface CommandExecutor {
    /**
     * @param command the command that is to be executed
     * @throws CommandExecutionException when the executor failed to execute the command
     */
    void execute(BaseCommand command) throws CommandExecutionException;
}
