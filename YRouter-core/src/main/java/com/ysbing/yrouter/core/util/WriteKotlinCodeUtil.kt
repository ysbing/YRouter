package com.ysbing.yrouter.core.util

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.ysbing.yrouter.core.DexBean
import com.ysbing.yrouter.core.util.WriteCodeUtil.getPackageNameAndClassName
import com.ysbing.yrouter.core.util.WriteCodeUtil.writeEmptyJava
import jadx.core.dex.nodes.ClassNode
import java.io.File
import java.lang.reflect.Type
import java.util.*

/**
 * 将解析后的dex，写入Java文件
 */
object WriteKotlinCodeUtil {
    fun writeKotlin(outPath: String, classNode: ClassNode, dexBeanList: List<DexBean>) {
        val saveFile = File(outPath, "main")
        val classBuilder = newClassBuilder(classNode, outPath)
        dexBeanList.map {
            beanInfo(classBuilder, it, outPath)
        }
        val file = FileSpec.builder(
            classNode.`package`,
            classNode.shortName
        ).addType(classBuilder.build()).build()
        file.writeTo(saveFile)
    }

    private fun newClassBuilder(classNode: ClassNode, outPath: String): TypeSpec.Builder {
        val classBuilder = TypeSpec.classBuilder(classNode.classInfo.shortName)
        classNode.superClass?.let {
            if (it.`object` != java.lang.Object::class.java.name) {
                val names = getPackageNameAndClassName(it.`object`)
                classBuilder.superclass(ClassName(names[0], names[1]).apply {
                    writeEmptyJava(outPath, packageName, simpleName)
                })
            }
        }
        return classBuilder
    }

    private fun beanInfo(classBuilder: TypeSpec.Builder, dexBean: DexBean, outPath: String) {
        if (dexBean.isMethod) {
            val method = dexBean.method
            classBuilder.addFunction(
                FunSpec.builder(method.name).apply {
                    method.argRegs.map argRegs@{ arg ->
                        if (arg.type.isPrimitive) {
                            addParameter(
                                arg.name,
                                getPrimitiveClassName(arg.type.toString()).first
                            )
                        } else {
                            val names = getPackageNameAndClassName(arg.type.`object`)
                            val className = ClassName(names[0], names[1]).apply {
                                writeEmptyJava(outPath, packageName, simpleName)
                            }
                            if (arg.type.isGeneric) {
                                val typeNames = ArrayList<ClassName>()
                                arg.type.genericTypes.map { genericType ->
                                    val genericTypeNames =
                                        getPackageNameAndClassName(genericType.`object`)
                                    typeNames.add(
                                        ClassName(
                                            genericTypeNames[0],
                                            genericTypeNames[1]
                                        ).apply {
                                            writeEmptyJava(outPath, packageName, simpleName)
                                        }
                                    )
                                }
                                addParameter(
                                    arg.name,
                                    className.parameterizedBy(typeNames).copy(true)
                                )
                            } else {
                                addParameter(
                                    arg.name,
                                    className.copy(true)
                                )
                            }
                        }
                    }
                    if (method.returnType != null) {
                        if (method.returnType.isPrimitive) {
                            val pair = getPrimitiveClassName(method.returnType.toString())
                            if (pair.first != Void::class.java) {
                                returns(pair.first)
                                addStatement("return ${pair.second}")
                            }
                        } else {
                            val names = getPackageNameAndClassName(method.returnType.`object`)
                            returns(ClassName(names[0], names[1]).apply {
                                writeEmptyJava(outPath, packageName, simpleName)
                            }.copy(true))
                            addStatement("return null")
                        }
                    }
                }.build()
            )
        } else if (dexBean.isField) {
            val field = dexBean.field
            if (field.type.isPrimitive) {
                val pair = getPrimitiveClassName(field.type.toString())
                classBuilder.addProperty(
                    PropertySpec.builder(
                        field.name,
                        pair.first
                    ).initializer(pair.second).mutable(true).build()
                )
            } else {
                val names = getPackageNameAndClassName(field.type.`object`)
                val className = ClassName(names[0], names[1]).apply {
                    writeEmptyJava(outPath, packageName, simpleName)
                }
                if (field.type.isGeneric) {
                    val typeNames = ArrayList<ClassName>()
                    field.type.genericTypes.map { genericType ->
                        val genericTypeNames = getPackageNameAndClassName(genericType.`object`)
                        typeNames.add(
                            ClassName(genericTypeNames[0], genericTypeNames[1]).apply {
                                writeEmptyJava(outPath, packageName, simpleName)
                            }
                        )
                    }
                    classBuilder.addProperty(
                        PropertySpec.builder(
                            field.name,
                            className.parameterizedBy(typeNames).copy(true)
                        ).initializer("null").mutable(true).build()
                    )
                } else {
                    classBuilder.addProperty(
                        PropertySpec.builder(field.name, className.copy(true)).initializer("null")
                            .mutable(true).build()
                    )
                }
            }
        } else if (dexBean.isInner) {
            val inner = dexBean.inner
            inner.keys.map {
                inner[it]?.groupBy { bean ->
                    bean.classNode
                }?.map { map ->
                    if (map.key.shortName == "Companion") {
                        val innerClassBuilder = TypeSpec.companionObjectBuilder()
                        map.value.map { bean ->
                            beanInfo(innerClassBuilder, bean, outPath)
                        }
                        classBuilder.addType(innerClassBuilder.build())
                    } else {
                        val innerClassBuilder = newClassBuilder(map.key, outPath)
                        map.value.map { bean ->
                            beanInfo(innerClassBuilder, bean, outPath)
                        }
                        classBuilder.addType(innerClassBuilder.build())
                    }
                }
            }
        }
    }

    private fun getPrimitiveClassName(typeName: String): Pair<Type, String> {
        return when (typeName) {
            "int" -> {
                Pair(Int::class.java, "0")
            }
            "float" -> {
                Pair(Float::class.java, "0f")
            }
            "double" -> {
                Pair(Double::class.java, "0f")
            }
            "long" -> {
                Pair(Long::class.java, "0L")
            }
            "void" -> {
                Pair(Void::class.java, "null")
            }
            else -> {
                Pair(Any::class.java, "null")
            }
        }
    }
}