package com.ysbing.yrouter.core

import com.ysbing.yrouter.api.YRouterSystem
import com.ysbing.yrouter.api.mock.YRouterMockBean
import com.ysbing.yrouter.api.mock.YRouterMockClass
import com.ysbing.yrouter.api.mock.YRouterMockValue
import jadx.api.JadxArgs
import jadx.core.dex.nodes.ClassNode
import jadx.core.dex.nodes.RootNode
import jadx.core.utils.files.InputFile
import java.io.File


class FindClass {

    private val root = RootNode(JadxArgs())
    private val loadedInputs = ArrayList<InputFile>()

    fun add(file: File) {
        try {
            InputFile.addFilesFrom(file, loadedInputs, true)
        } catch (e: Throwable) {
        }
    }

    fun load() {
        root.load(loadedInputs)
    }

    private fun searchClassByName(className: String): ClassNode? {
        return root.searchClassByName(className)
    }

    fun hasClass(className: String): Boolean {
        return searchClassByName(className) != null
    }

    fun hasYRouterSystem(className: String): Boolean {
        val classNode = searchClassByName(className) ?: return false
        return classNode.getAnnotation(YRouterSystem::class.java.name) != null
    }

    fun getMockClassArray(): List<YRouterMockBean> {
        val findClass = ArrayList<YRouterMockBean>()
        root.getClasses(false).map { classNode ->
            val classAnnotation = classNode.getAnnotation(YRouterMockClass::class.java.name)
            if (classAnnotation != null) {
                classNode.fields.map { field ->
                    val fieldAnnotation = field.getAnnotation(YRouterMockValue::class.java.name)
                    if (fieldAnnotation != null) {
                        findClass.add(YRouterMockBean().apply {
                            className = classNode.fullName
                            targetClass = classAnnotation.defaultValue.toString()
                            name = field.name
                            targetName = fieldAnnotation.defaultValue.toString()
                            type = YRouterMockBean.TYPE.FIELD
                        })
                    }
                }
                classNode.methods.map { method ->
                    val methodAnnotation = method.getAnnotation(YRouterMockValue::class.java.name)
                    if (methodAnnotation != null) {
                        findClass.add(YRouterMockBean().apply {
                            className = classNode.fullName
                            targetClass = classAnnotation.defaultValue.toString()
                            name = method.name
                            targetName = methodAnnotation.defaultValue.toString()
                            type = YRouterMockBean.TYPE.METHOD
                        })
                    }
                }
            }
        }
        return findClass
    }
}