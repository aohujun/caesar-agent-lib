package com.navercorp.pinpoint.bootstrap.plugin.jdbc;

import java.util.Map;

/**
 * @author Jongho Moon
 *
 */
public interface BindValueAccessor {
    void _$PINPOINT$_setBindValue(Map<Integer, String> map);
    Map<Integer, String> _$PINPOINT$_getBindValue();
}
