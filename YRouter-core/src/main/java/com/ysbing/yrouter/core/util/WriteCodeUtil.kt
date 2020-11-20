package com.ysbing.yrouter.core.util

import com.squareup.javapoet.JavaFile
import com.squareup.javapoet.TypeSpec
import com.squareup.javapoet.TypeVariableName
import com.ysbing.yrouter.core.DexBean
import jadx.core.dex.attributes.AType
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
        try {
            if (isKotlin(classNode)) {
                WriteKotlinCodeUtil(outPath, classNode, dexBeanList).writeKotlin()
            } else {
                WriteJavaCodeUtil(outPath, classNode, dexBeanList).writeJava()
            }
        } catch (e: Exception) {
            println("代码生成失败：$classNode")
            WriteJavaCodeUtil(outPath, classNode, dexBeanList).writeJava()
            throw e
        }
    }

    companion object {
        private const val sKotlinFlag = "kotlin.Metadata"
        private val sClassMap = HashMap<String, InnerClass>()

        fun isKotlin(classNode: ClassNode): Boolean {
            val sourceFile = classNode.get(AType.SOURCE_FILE)
            return (classNode.getAnnotation(sKotlinFlag) != null
                    || (sourceFile != null && sourceFile.fileName.endsWith(".kt")))
        }

        fun getKotlinPackageNameAndClassName(name: String): Array<String> {
            return getPackageNameAndClassName(JavaToKotlinObj.javaToKotlin(name))
        }

        fun getPackageNameAndClassName(name: String): Array<String> {
            return if (name.contains(".")) {
                arrayOf(name.substringBeforeLast("."), name.substringAfterLast("."))
            } else {
                arrayOf("", name)
            }
        }

        fun writeEmptyJava(
            outPath: String,
            packageName: String,
            classNames: List<String>,
            isInterface: Boolean,
            genericCount: Int
        ) {
            if (classNames.isEmpty()) {
                return
            }
            if (JavaToKotlinObj.contains("$packageName.${classNames[0]}")) {
                return
            }
            val innerClass = sClassMap.getOrDefault(packageName + classNames[0], InnerClass())
            var childInnerClass: InnerClass? = null
            classNames.map {
                if (childInnerClass == null) {
                    childInnerClass = innerClass
                    childInnerClass?.name = it
                    if (it == classNames[classNames.size - 1]) {
                        if (isInterface) {
                            childInnerClass?.isInterface = isInterface
                        }
                        if (genericCount > childInnerClass?.genericCount ?: 0) {
                            childInnerClass?.genericCount = genericCount
                        }
                    }
                } else {
                    val inner = InnerClass()
                    inner.name = it
                    inner.isInterface = isInterface
                    inner.genericCount = genericCount
                    if (childInnerClass?.inner?.contains(inner) != true) {
                        childInnerClass?.inner?.add(inner)
                    } else {
                        childInnerClass?.inner?.get(
                            childInnerClass?.inner?.indexOf(inner) ?: return@map
                        )?.apply {
                            if (isInterface) {
                                this.isInterface = isInterface
                            }
                            if (genericCount > this.genericCount) {
                                this.genericCount = genericCount
                            }
                        }
                    }
                    childInnerClass = inner
                }
            }
            sClassMap[packageName + classNames[0]] = innerClass
            val saveFile = File(outPath, "lib")

            fun write(innerClass: InnerClass): TypeSpec.Builder {
                val builder = if (innerClass.isInterface) {
                    TypeSpec.interfaceBuilder(innerClass.name)
                } else {
                    TypeSpec.classBuilder(innerClass.name)
                }.addModifiers(Modifier.PUBLIC)
                innerClass.inner.map {
                    builder.addType(write(it).addModifiers(Modifier.STATIC).build())
                }
                for (i in 0 until innerClass.genericCount) {
                    builder.addTypeVariable(TypeVariableName.get("T$i"))
                }
                return builder
            }
            sClassMap[packageName + classNames[0]]?.let {
                write(it).let { classBuilder ->
                    val javaFile: JavaFile =
                        JavaFile.builder(packageName, classBuilder.build()).build()
                    javaFile.writeTo(saveFile)
                }
            }
        }

        fun getSafeMethodName(arg: RegisterArg): String {
            if (arg.name != null) {
                return arg.name
            }
            return "arg" + System.nanoTime()
        }
    }

    class InnerClass {
        val inner = ArrayList<InnerClass>()
        var name: String? = null
        var isInterface: Boolean = false
        var genericCount: Int = 0
        override fun equals(other: Any?): Boolean {
            if (this === other) {
                return true
            }
            if (other == null) {
                return false
            }
            return toString() == other.toString()
        }

        override fun toString(): String {
            return name ?: ""
        }

        override fun hashCode(): Int {
            return toString().hashCode()
        }
    }
}