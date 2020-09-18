package com.ysbing.yrouter.core.util

import com.squareup.javapoet.JavaFile
import com.ysbing.yrouter.core.DexBean
import com.ysbing.yrouter.core.util.WriteJavaCodeUtil.writeJava
import com.ysbing.yrouter.core.util.WriteKotlinCodeUtil.writeKotlin
import jadx.core.dex.nodes.ClassNode
import java.io.File
import javax.lang.model.element.Modifier

/**
 * 将解析后的dex，写入Java文件
 */
object WriteCodeUtil {
    private const val sKotlinFlag = "kotlin.Metadata"
    fun run(outPath: String, classNode: ClassNode, dexBeanList: List<DexBean>) {
        if (classNode.getAnnotation(sKotlinFlag) == null) {
            writeJavaPrimitive(outPath)
            writeJava(outPath, classNode, dexBeanList)
        } else {
            writeKotlinPrimitive(outPath)
            writeKotlin(outPath, classNode, dexBeanList)
        }
    }

    fun getPackageNameAndClassName(name: String): Array<String> {
        return arrayOf(name.substringBeforeLast("."), name.substringAfterLast("."))
    }

    private fun writeJavaPrimitive(outPath: String) {
        writeEmptyJava(outPath, "java.lang", "String")
        writeEmptyJava(outPath, "java.lang", "Object")
    }

    private fun writeKotlinPrimitive(outPath: String) {
        writeEmptyJava(outPath, "kotlin", "Int")
        writeEmptyJava(outPath, "kotlin", "Float")
        writeEmptyJava(outPath, "kotlin", "Double")
        writeEmptyJava(outPath, "kotlin", "Long")
        writeEmptyJava(outPath, "kotlin", "Any")
    }

    fun writeEmptyJava(outPath: String, packageName: String, className: String) {
        val saveFile = File(outPath, "lib")
        val helloWorld = com.squareup.javapoet.TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
            .build()
        val javaFile: JavaFile = JavaFile.builder(packageName, helloWorld)
            .build()
        javaFile.writeTo(saveFile)
    }
}