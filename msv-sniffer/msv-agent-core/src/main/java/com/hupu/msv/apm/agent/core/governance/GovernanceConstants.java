package com.hupu.msv.apm.agent.core.governance;

/**
 * @author: zhaoxudong
 * @date: 2019-10-31 15:37
 * @description:
 */
public class GovernanceConstants {
    /**
     * 是否开启规则：关闭
     */
    public static final int RULE_ENABLED_CLOSE = 0;

    /**
     * 是否开启规则：开启
     */
    public static final int RULE_ENABLED_OPEN = 1;

    /**
     * 规则是否删除：正常
     */
    public static final int RULE_DEL_FLAG_NORMAL = 0;

    /**
     * 规则是否删除：删除
     */
    public static final int RULE_DEL_FLAG_DEL = 1;

    /**
     * 服务治理统计前缀
     */
    public static final String BIZ_METER_NAME_PREFIX = "caesar_biz_governance";

    public static final String CAESAR_DEFAULT_VERSION = "0.0.2";

}
