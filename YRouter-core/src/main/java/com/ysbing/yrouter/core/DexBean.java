package com.ysbing.yrouter.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import jadx.core.dex.instructions.args.ArgType;
import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;

public class DexBean {
    public ClassNode classNode;
    public ClassType classType;
    public NodeType nodeType;
    public MethodNode method;
    public FieldNode field;
    public Set<String> interfaceInfo = new HashSet<>();
    public List<ArgType> superInfo1 = new ArrayList<>();
    public List<String> superInfo2 = new ArrayList<>();
    public Map<ClassNode, Set<DexBean>> inner = new HashMap<>();

    public enum ClassType {
        CLASS, INTERFACE, OBJECT, ENUM
    }

    public enum NodeType {
        METHOD, FIELD, INNER
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof DexBean)) return false;
        DexBean dexBean = (DexBean) o;
        return Objects.equals(classNode, dexBean.classNode) &&
                classType == dexBean.classType &&
                nodeType == dexBean.nodeType &&
                Objects.equals(method, dexBean.method) &&
                Objects.equals(field, dexBean.field);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classNode, classType, nodeType, method, field);
    }
}
