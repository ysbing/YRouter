package com.ysbing.yrouter.core

import com.ysbing.yrouter.api.YRouterApi
import jadx.api.JadxArgs
import jadx.core.dex.nodes.ClassNode
import jadx.core.dex.nodes.FieldNode
import jadx.core.dex.nodes.MethodNode
import jadx.core.dex.nodes.RootNode
import jadx.core.dex.visitors.DepthTraversal
import jadx.core.dex.visitors.IDexTreeVisitor
import jadx.core.utils.files.InputFile
import java.io.File


object ExtractDexClassObject {

    fun run(file: File, infoList: MutableList<DexBean>) {
        println("yrouter begin extract from class:$file")
        val list = ArrayList<DexBean>()
        println(file)
        val root = RootNode(JadxArgs())
        val loadedInputs = ArrayList<InputFile>()
        InputFile.addFilesFrom(file, loadedInputs, true)
        root.load(loadedInputs)
        root.initClassPath()
        root.initPasses()
        root.getClasses(false).map { classNode ->
            extract(classNode, list, null)
        }
        fun visit(visitor: IDexTreeVisitor, bean: DexBean) {
            if (bean.nodeType == DexBean.NodeType.INNER) {
                bean.inner.values.map {
                    it.map {
                        visit(visitor, it)
                    }
                }
            }
            bean.method?.let { method ->
                method.load()
                DepthTraversal.visit(visitor, method)
            }
        }
        list.groupBy {
            it.classNode
        }.map {
            for (visitor in root.passes) {
                it.value.map { bean ->
                    visit(visitor, bean)
                }
            }
        }
        infoList.addAll(list)
    }

    private fun extract(
        classNode: ClassNode,
        list: MutableList<DexBean>,
        parentClassNode: ClassNode?,
        extractClass: Boolean = false
    ) {
        fun extractField(field: FieldNode) {
            println("yrouter extractField:$field")
            val dexBean = DexBean()
            dexBean.classNode = classNode
            dexBean.classType = getClassTypeFromClassNode(classNode).apply {
                if (this == DexBean.ClassType.UNKNOWN) {
                    return
                }
            }
            dexBean.nodeType = DexBean.NodeType.FIELD
            dexBean.field = field
            if (parentClassNode == null) {
                list.add(dexBean)
            } else {
                val bean = list.find {
                    it.classNode == parentClassNode && it.nodeType == DexBean.NodeType.INNER && it.inner.containsKey(
                        classNode
                    )
                }
                if (bean == null) {
                    val innerBean = DexBean()
                    innerBean.classNode = parentClassNode
                    innerBean.classType = getClassTypeFromClassNode(parentClassNode).apply {
                        if (this == DexBean.ClassType.UNKNOWN) {
                            return
                        }
                    }
                    innerBean.nodeType = DexBean.NodeType.INNER
                    if (innerBean.inner[classNode] == null) {
                        innerBean.inner[classNode] = ArrayList()
                    }
                    innerBean.inner[classNode]?.add(dexBean)
                    list.add(innerBean)
                } else {
                    if (bean.inner[classNode] == null) {
                        bean.inner[classNode] = ArrayList()
                    }
                    bean.inner[classNode]?.add(dexBean)
                }
            }
        }

        fun extractMethod(method: MethodNode) {
            println("yrouter extractMethod:$method")
            val dexBean = DexBean()
            dexBean.classNode = classNode
            dexBean.classType = getClassTypeFromClassNode(classNode).apply {
                if (this == DexBean.ClassType.UNKNOWN) {
                    return
                }
            }
            dexBean.nodeType = DexBean.NodeType.METHOD
            dexBean.method = method
            if (parentClassNode == null) {
                list.add(dexBean)
            } else {
                val bean = list.find {
                    it.classNode == parentClassNode && it.nodeType == DexBean.NodeType.INNER && it.inner.containsKey(
                        classNode
                    )
                }
                if (bean == null) {
                    val innerBean = DexBean()
                    innerBean.classNode = parentClassNode
                    innerBean.classType = getClassTypeFromClassNode(parentClassNode).apply {
                        if (this == DexBean.ClassType.UNKNOWN) {
                            return
                        }
                    }
                    innerBean.nodeType = DexBean.NodeType.INNER
                    if (innerBean.inner[classNode] == null) {
                        innerBean.inner[classNode] = ArrayList()
                    }
                    innerBean.inner[classNode]?.add(dexBean)
                    list.add(innerBean)
                } else {
                    if (bean.inner[classNode] == null) {
                        bean.inner[classNode] = ArrayList()
                    }
                    bean.inner[classNode]?.add(dexBean)
                }
            }
        }
        if (classNode.getAnnotation(YRouterApi::class.java.name) != null || extractClass) {
            classNode.fields.map annotation@{ field ->
                if (checkField(field)) {
                    extractField(field)
                }
            }
            classNode.methods.map annotation@{ method ->
                if (checkMethod(method)) {
                    extractMethod(method)
                }
            }
            classNode.innerClasses.map { innerClass ->
                if (checkClass(innerClass)) {
                    extract(innerClass, list, classNode, true)
                }
            }
        } else {
            classNode.fields.map annotation@{ field ->
                if (field.getAnnotation(YRouterApi::class.java.name) != null && checkField(field)) {
                    extractField(field)
                }
            }
            classNode.methods.map annotation@{ method ->
                if (method.getAnnotation(YRouterApi::class.java.name) != null && checkMethod(method)) {
                    extractMethod(method)
                }
            }
            classNode.innerClasses.map { innerClass ->
                if (checkClass(innerClass)) {
                    extract(innerClass, list, classNode)
                }
            }
        }
    }

    fun getClassTypeFromClassNode(classNode: ClassNode): DexBean.ClassType {
        return when {
            classNode.accessFlags.isInterface -> {
                DexBean.ClassType.INTERFACE
            }
            classNode.fields.find { it.name == "INSTANCE" } != null -> {
                DexBean.ClassType.OBJECT
            }
            classNode.isEnum -> {
                DexBean.ClassType.UNKNOWN
            }
            else -> DexBean.ClassType.CLASS
        }
    }

    private fun checkClass(classNode: ClassNode): Boolean {
        return classNode.shortName != "DefaultImpls"
    }

    private fun checkField(fieldNode: FieldNode): Boolean {
        return fieldNode.name != "INSTANCE"
                && fieldNode.name != "Companion"
                && !fieldNode.name.contains("_\$_")
                && fieldNode.parentClass.shortName != "DefaultImpls"
                && !fieldNode.parentClass.shortName.contains("AnonymousClass")
    }

    private fun checkMethod(methodNode: MethodNode): Boolean {
        return methodNode.name != "<clinit>"
                && methodNode.name != "<init>"
                && !methodNode.name.contains("_\$_")
                && !methodNode.name.contains("access\$")
                && methodNode.parentClass.shortName != "DefaultImpls"
                && !methodNode.parentClass.shortName.contains("AnonymousClass")
                && methodNode.parentClass.fields.find {
            it.name.capitalize() == methodNode.name.substringAfter("set")
                    || it.name.capitalize() == methodNode.name.substringAfter("get")
        } == null
    }
}