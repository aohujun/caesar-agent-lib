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


package com.hupu.msv.apm.plugin.jdbc.trace;

import java.sql.SQLException;
import com.hupu.msv.apm.agent.core.context.tag.Tags;
import com.hupu.msv.apm.agent.core.context.trace.AbstractSpan;
import com.hupu.msv.apm.agent.core.context.trace.SpanLayer;
import com.hupu.msv.apm.agent.core.context.ContextManager;

/**
 * {@link PreparedStatementTracing} create an exit span when the client call the method in the class that extend {@link
 * java.sql.PreparedStatement}.
 *
 * @author zhangxin
 */
public class PreparedStatementTracing {

    public static <R> R execute(java.sql.PreparedStatement realStatement,
        ConnectionInfo connectInfo, String method, String sql, Executable<R> exec)
        throws SQLException {
        final AbstractSpan span = ContextManager.createExitSpan(connectInfo.getDBType() + "/JDBI/PreparedStatement/" + method, connectInfo.getDatabasePeer());
        try {
            Tags.DB_TYPE.set(span, "sql");
            Tags.DB_INSTANCE.set(span, connectInfo.getDatabaseName());
            Tags.DB_STATEMENT.set(span, sql);
            span.setComponent(connectInfo.getComponent());

            SpanLayer.asDB(span);
            return exec.exe(realStatement, sql);
        } catch (SQLException e) {
            span.errorOccurred();
            span.log(e);
            throw e;
        } finally {
            ContextManager.stopSpan(span);
        }
    }

    public interface Executable<R> {
        R exe(java.sql.PreparedStatement realConnection, String sql)
            throws SQLException;
    }
}
