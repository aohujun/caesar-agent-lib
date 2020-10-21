package com.hupu.msv.apm.agent.core.governance.gray.strategy.predicate;

import com.hupu.msv.apm.agent.core.governance.gray.config.GrayTag;

import java.util.HashMap;

/**
 * @author: Aile
 * @create: 2019/11/04 19:09
 */
public class GrayRule {

    private HashMap<String, GrayTag> grayTag = new HashMap<>();

    private long id;

    private String color;

    /**
     * 命中条件
     */
    private String condition;


    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }


    public HashMap<String, GrayTag> getGrayTag() {
        return grayTag;
    }

    public void setGrayTag(HashMap<String, GrayTag> grayTag) {
        this.grayTag = grayTag;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }
}
