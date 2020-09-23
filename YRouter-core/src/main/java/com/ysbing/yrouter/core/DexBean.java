package com.ysbing.yrouter.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;

public class DexBean {
    public ClassNode classNode;
    public ClassType classType;
    public NodeType nodeType;
    public MethodNode method;
    public FieldNode field;
    public Map<ClassNode, List<DexBean>> inner = new HashMap<>();

    public enum ClassType {
        CLASS, INTERFACE, OBJECT, UNKNOWN
    }

    public enum NodeType {
        METHOD, FIELD, INNER
    }
}
