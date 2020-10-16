package com.ysbing.yrouter.api.mock;

public class YRouterMockBean {
    public String className;
    public String targetClass;
    public String name;
    public String targetName;
    public TYPE type;

    public enum TYPE {
        FIELD, METHOD
    }
}