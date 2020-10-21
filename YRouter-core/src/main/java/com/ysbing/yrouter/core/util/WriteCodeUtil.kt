package com.ysbing.yrouter.core.util

import com.squareup.javapoet.JavaFile
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
class WriteCodeUtil(private val outPath: String) {

    init {
        WriteJavaMockCodeUtil.writeEmptyMock(outPath)
    }

    fun run(classNode: ClassNode, dexBeanList: List<DexBean>) {
        if (classNode.getAnnotation(sKotlinFlag) == null) {
            writeJava(outPath, classNode, dexBeanList)
        } else {
            writeKotlin(outPath, classNode, dexBeanList)
        }
    }

    companion object {
        private const val sKotlinFlag = "kotlin.Metadata"
        fun getPackageNameAndClassName(name: String): Array<String> {
            return arrayOf(name.substringBeforeLast("."), name.substringAfterLast("."))
        }

        fun writeEmptyJava(
            outPath: String,
            packageName: String,
            className: String,
            generic: Boolean = false
        ) {
            if (packageName.startsWith("java.")) {
                return
            }
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

}