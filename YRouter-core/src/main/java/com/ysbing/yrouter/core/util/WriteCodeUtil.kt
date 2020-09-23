package com.ysbing.yrouter.core.util

import com.squareup.javapoet.ClassName
import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.ParameterizedTypeName
import com.squareup.javapoet.TypeVariableName
import com.ysbing.yrouter.core.DexBean
import com.ysbing.yrouter.core.util.WriteJavaCodeUtil.writeJava
import com.ysbing.yrouter.core.util.WriteKotlinCodeUtil.writeKotlin
import jadx.core.dex.instructions.args.ArgType
import jadx.core.dex.instructions.args.RegisterArg
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
        writeEmptyJava(outPath, "kotlin", "Boolean")
        writeEmptyJava(outPath, "kotlin", "Char")
        writeEmptyJava(outPath, "kotlin", "Byte")
        writeEmptyJava(outPath, "kotlin", "Short")
        writeEmptyJava(outPath, "kotlin", "Int")
        writeEmptyJava(outPath, "kotlin", "Float")
        writeEmptyJava(outPath, "kotlin", "Long")
        writeEmptyJava(outPath, "kotlin", "Double")
        writeEmptyJava(outPath, "kotlin", "String")
        writeEmptyJava(outPath, "kotlin", "Any")
        writeEmptyJava(outPath, "kotlin", "Array", true)
    }

    fun writeEmptyJava(
        outPath: String,
        packageName: String,
        className: String,
        generic: Boolean = false
    ) {
        val saveFile = File(outPath, "lib")
        val classBuilder = com.squareup.javapoet.TypeSpec.classBuilder(className)
            .addModifiers(Modifier.PUBLIC)
        if (generic) {
            classBuilder.addTypeVariable(TypeVariableName.get("T"))
        }
        val javaFile: JavaFile = JavaFile.builder(packageName, classBuilder.build())
            .build()
        javaFile.writeTo(saveFile)
    }

    fun getSafeMethodName(arg: RegisterArg): String {
        if (arg.name != null) {
            return arg.name
        }
        val argType =
            if (arg.type != ArgType.UNKNOWN) arg.type else arg.initType
        return try {
            argType.`object`.substringAfterLast(".").decapitalize()
        } catch (e: java.lang.UnsupportedOperationException) {
            "field" + System.currentTimeMillis().toString()
        }
    }
}