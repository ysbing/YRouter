package com.ysbing.yrouter.core;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jadx.core.dex.nodes.ClassNode;
import jadx.core.dex.nodes.FieldNode;
import jadx.core.dex.nodes.MethodNode;

public class DexBean {
    public ClassNode classNode;
    public boolean isMethod;
    public boolean isField;
    public boolean isInner;
    public MethodNode method;
    public FieldNode field;
    public Map<ClassNode, List<DexBean>> inner = new HashMap<>();
}
