package com.navercorp.pinpoint.bootstrap.plugin.jdbc;

import com.navercorp.pinpoint.bootstrap.context.ParsingResult;

/**
 * @author Jongho Moon
 *
 */
public interface ParsingResultAccessor {
    void _$PINPOINT$_setParsingResult(ParsingResult result);
    ParsingResult _$PINPOINT$_getParsingResult();
}
