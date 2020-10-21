package com.hupu.msv.apm.agent.core.governance.gray.config;

import java.util.List;

/**
 * @description: 灰度路由标签
 * @author: Aile
 * @create: 2019/11/11 10:13
 */
public class GrayTag {

    private String key;
    private List<String> values;
    private List<Integer> weights;
    private List<Integer> weightSoFars;

    public GrayTag() {
    }

    public GrayTag(String key, List<String> values, List<Integer> weightSoFars) {
        this.key = key;
        this.values = values;
        this.weightSoFars = weightSoFars;
    }

    public GrayTag(String key, List<String> values) {
        this.key = key;
        this.values = values;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public List<Integer> getWeightSoFars() {
        return weightSoFars;
    }

    public void setWeightSoFars(List<Integer> weightSoFars) {
        this.weightSoFars = weightSoFars;
    }

    public List<Integer> getWeights() {
        return weights;
    }

    public void setWeights(List<Integer> weight) {
        this.weights = weight;
    }

}
