package com.hupu.msv.apm.agent.core.governance;

/**
 * @author: zhaoxudong
 * @date: 2019-12-05 21:49
 * @description:
 */
public enum GovernanceEventStatusEnum {
    /**
     * 开启
     */
    OPEN(1),
    /**
     * 关闭
     */
    CLOSE(0);
    private int code;

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    GovernanceEventStatusEnum(int code) {
        this.code = code;
    }
}
