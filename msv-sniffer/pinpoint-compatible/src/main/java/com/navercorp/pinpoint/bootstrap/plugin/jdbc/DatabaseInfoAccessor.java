package com.navercorp.pinpoint.bootstrap.plugin.jdbc;

import com.navercorp.pinpoint.bootstrap.context.DatabaseInfo;

/**
 * @author Jongho Moon
 *
 */
public interface DatabaseInfoAccessor {
    void _$PINPOINT$_setDatabaseInfo(DatabaseInfo info);
    DatabaseInfo _$PINPOINT$_getDatabaseInfo();
}
