package com.hupu.msv.apm.agent.core.governance.gray.config;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @description: 灰度路由配置项
 * @author: Aile
 * @create: 2019/11/04 19:09
 */
public class GrayRuleConfig {

    /**
     * <key=header、reqParms或其他 , <字段key，字段值列表>>
     */
    private Predicate predicate;
    private List<Rule> rules;
    private boolean delete = false;
    private boolean enable = true;
    private long id;
    private String color;

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isDelete() {
        return delete;
    }

    public void setDelete(boolean delete) {
        this.delete = delete;
    }

    public List<Rule> getRules() {
        return rules;
    }

    public void setRules(List<Rule> rules) {
        this.rules = rules;
    }

    public Predicate getPredicate() {
        return predicate;
    }

    public void setPredicate(Predicate predicate) {
        this.predicate = predicate;
    }

    public boolean isEnable() {
        return enable;
    }

    public void setEnable(boolean enable) {
        this.enable = enable;
    }

    public static class Rule extends GrayTag {
        private String serviceId;

        public String getServiceId() {
            return serviceId;
        }

        public void setServiceId(String serviceId) {
            this.serviceId = serviceId;
        }
    }


    public static final class Predicate {
        private HashMap<String, List<String>> queryString;
        private ArrayList<String> paths;
        private HashMap<String, List<String>> header;
        private HashMap<String, List<String>> postData;

        public HashMap<String, List<String>> getQueryString() {
            return queryString;
        }

        public void setQueryString(HashMap<String, List<String>> queryString) {
            this.queryString = queryString;
        }

        public ArrayList<String> getPaths() {
            return paths;
        }

        public void setPaths(ArrayList<String> paths) {
            this.paths = paths;
        }

        public HashMap<String, List<String>> getHeader() {
            return header;
        }

        public void setHeader(HashMap<String, List<String>> header) {
            this.header = header;
        }

        public HashMap<String, List<String>> getPostData() {
            return postData;
        }

        public void setPostData(HashMap<String, List<String>> postData) {
            this.postData = postData;
        }
    }


}


