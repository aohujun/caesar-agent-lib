package com.hupu.msv.apm.plugin.log.common;

import com.hupu.msv.apm.agent.core.util.CaesarAgentUtil;

/**
 * @author: zhaoxudong
 * @date: 2020-07-29 11:38
 * @description: 日志工具包
 */
public class LogUtil {

    /**
     * 获取日志中打印输出的traceID
     * 未cai dao
     * @param traceId
     * @return
     */
    public static String getTid(String traceId) {
        if(CaesarAgentUtil.isSampled(traceId)){
            return traceId;
        }
        return "~"+traceId;
    }

}
