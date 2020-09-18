package com.ysbing.yrouter.core

import com.ysbing.yrouter.api.YRouterApi
import jadx.api.JadxArgs
import jadx.core.dex.nodes.ClassNode
import jadx.core.dex.nodes.RootNode
import jadx.core.dex.visitors.DepthTraversal
import jadx.core.dex.visitors.IDexTreeVisitor
import jadx.core.utils.files.InputFile
import java.io.File


object ExtractDexClass {

    fun run(file: File, infoList: MutableList<DexBean>) {
        println("开始收集:$file")
        val list = ArrayList<DexBean>()
        println(file)
        val args = JadxArgs()
        args.setInputFile(file)
        val loadedInputs = ArrayList<InputFile>()
        InputFile.addFilesFrom(file, loadedInputs, true)
        val root = RootNode(args)
        root.load(loadedInputs)
        root.initClassPath()
        root.initPasses()
        root.getClasses(false).map { classNode ->
            collect(classNode, list, null)
        }
        fun visit(visitor: IDexTreeVisitor, bean: DexBean) {
            if (bean.isInner) {
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

    private fun collect(
        classNode: ClassNode,
        list: MutableList<DexBean>,
        parentClassNode: ClassNode?
    ) {
        classNode.fields.map annotation@{ field ->
            if (field.getAnnotation(YRouterApi::class.java.name) != null) {
                println("变量:$field")
                val dexBean = DexBean()
                dexBean.classNode = classNode
                dexBean.isField = true
                dexBean.field = field
                if (parentClassNode == null) {
                    list.add(dexBean)
                } else {
                    val bean = list.find {
                        it.classNode == parentClassNode && it.isInner && it.inner.containsKey(
                            classNode
                        )
                    }
                    if (bean == null) {
                        val innerBean = DexBean()
                        innerBean.classNode = parentClassNode
                        innerBean.isInner = true
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
                return@annotation
            }
        }
        classNode.methods.map annotation@{ method ->
            if (method.getAnnotation(YRouterApi::class.java.name) != null) {
                println("方法:$method")
                val dexBean = DexBean()
                dexBean.classNode = classNode
                dexBean.isMethod = true
                dexBean.method = method
                if (parentClassNode == null) {
                    list.add(dexBean)
                } else {
                    val bean = list.find {
                        it.classNode == parentClassNode && it.isInner && it.inner.containsKey(
                            classNode
                        )
                    }
                    if (bean == null) {
                        val innerBean = DexBean()
                        innerBean.classNode = parentClassNode
                        innerBean.isInner = true
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
                return@annotation
            }
        }
        classNode.innerClasses.map { innerClass ->
            collect(innerClass, list, classNode)
        }
    }
}