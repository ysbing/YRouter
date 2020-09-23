package com.ysbing.yrouter.core.util

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.ysbing.yrouter.core.DexBean
import com.ysbing.yrouter.core.ExtractDexClass
import com.ysbing.yrouter.core.util.WriteCodeUtil.getPackageNameAndClassName
import com.ysbing.yrouter.core.util.WriteCodeUtil.writeEmptyJava
import jadx.core.dex.instructions.args.ArgType
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
        val classType = ExtractDexClass.getClassTypeFromClassNode(classNode)
        val classBuilder = when (classType) {
            DexBean.ClassType.OBJECT -> {
                TypeSpec.objectBuilder(classNode.classInfo.shortName)
            }
            DexBean.ClassType.INTERFACE -> {
                TypeSpec.interfaceBuilder(classNode.classInfo.shortName)
            }
            else -> {
                TypeSpec.classBuilder(classNode.classInfo.shortName)
            }
        }
        if (classType != DexBean.ClassType.INTERFACE && classNode.accessFlags.isAbstract) {
            classBuilder.addModifiers(KModifier.ABSTRACT)
        }
        if (classType == DexBean.ClassType.CLASS) {
            classNode.superClass?.let {
                if (it.`object` != java.lang.Object::class.java.name) {
                    val names = getPackageNameAndClassName(it.`object`)
                    classBuilder.superclass(ClassName(names[0], names[1]).apply {
                        writeEmptyJava(outPath, packageName, simpleName)
                    })
                }
            }
        }
        return classBuilder
    }

    private fun beanInfo(classBuilder: TypeSpec.Builder, dexBean: DexBean, outPath: String) {
        if (dexBean.nodeType == DexBean.NodeType.METHOD) {
            val method = dexBean.method
            classBuilder.addFunction(
                FunSpec.builder(method.name).apply {
                    method.argRegs.map argRegs@{ arg ->
                        if (arg.type.isPrimitive) {
                            addParameter(
                                WriteCodeUtil.getSafeMethodName(arg),
                                getPrimitiveClassName(arg.type.toString()).first
                            )
                        } else {
                            val argType =
                                if (arg.type != ArgType.UNKNOWN) arg.type else arg.initType
                            if (argType.isGeneric) {
                                val names = getPackageNameAndClassName(argType.`object`)
                                val className = ClassName(names[0], names[1]).apply {
                                    writeEmptyJava(outPath, packageName, simpleName)
                                }
                                val typeNames = ArrayList<ClassName>()
                                argType.genericTypes.map { genericType ->
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
                                    WriteCodeUtil.getSafeMethodName(arg),
                                    className.parameterizedBy(typeNames).copy(true)
                                )
                            } else {
                                val className = if (argType.isArray) {
                                    if (argType.arrayElement.isPrimitive) {
                                        val pair =
                                            getPrimitiveClassName(argType.arrayElement.toString())
                                        val array = ClassName("kotlin", "Array")
                                        array.parameterizedBy(pair.first)
                                    } else {
                                        val names =
                                            getPackageNameAndClassName(argType.arrayElement.`object`)
                                        val array = ClassName("kotlin", "Array")
                                        array.parameterizedBy(
                                            ClassName(names[0], names[1]).apply {
                                                writeEmptyJava(outPath, packageName, simpleName)
                                            })
                                    }
                                } else {
                                    val names = getPackageNameAndClassName(argType.`object`)
                                    ClassName(names[0], names[1]).apply {
                                        writeEmptyJava(outPath, packageName, simpleName)
                                    }
                                }
                                addParameter(
                                    WriteCodeUtil.getSafeMethodName(arg),
                                    className.copy(true)
                                )
                            }
                        }
                    }
                    if (method.returnType != null) {
                        if (method.returnType.isPrimitive) {
                            val pair = getPrimitiveClassName(method.returnType.toString())
                            if (pair.first != Void::class.asTypeName()) {
                                if (pair.first == Any::class.java) {
                                    returns(ClassName("", Any::class.java.name).copy(true))
                                } else {
                                    returns(pair.first)
                                }
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
        } else if (dexBean.nodeType == DexBean.NodeType.FIELD) {
            val field = dexBean.field
            if (field.type.isObject && field.type.`object` == String::class.java.name || field.type.isPrimitive) {
                val pair = getPrimitiveClassName(field.type.toString())
                classBuilder.addProperty(
                    PropertySpec.builder(
                        field.name,
                        pair.first
                    ).apply {
                        initializer(pair.second)
                        val classType = ExtractDexClass.getClassTypeFromClassNode(field.parentClass)
                        if (classType == DexBean.ClassType.OBJECT && field.accessFlags.isFinal) {
                            addModifiers(KModifier.CONST)
                        } else {
                            mutable(true)
                        }
                    }.build()
                )
            } else {
                if (field.type.isGeneric) {
                    val names = getPackageNameAndClassName(field.type.`object`)
                    val className = ClassName(names[0], names[1]).apply {
                        writeEmptyJava(outPath, packageName, simpleName)
                    }
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
                        ).apply {
                            initializer("null")
                            mutable(true)
                        }.build()
                    )
                } else {
                    val className = if (field.type.isArray) {
                        if (field.type.arrayElement.isPrimitive) {
                            val pair = getPrimitiveClassName(field.type.arrayElement.toString())
                            val array = ClassName("kotlin", "Array")
                            array.parameterizedBy(pair.first)
                        } else {
                            val names =
                                getPackageNameAndClassName(field.type.arrayElement.`object`)
                            val array = ClassName("kotlin", "Array")
                            array.parameterizedBy(
                                ClassName(names[0], names[1]).apply {
                                    writeEmptyJava(outPath, packageName, simpleName)
                                })
                        }
                    } else {
                        val names = getPackageNameAndClassName(field.type.`object`)
                        ClassName(names[0], names[1]).apply {
                            writeEmptyJava(outPath, packageName, simpleName)
                        }
                    }
                    classBuilder.addProperty(
                        PropertySpec.builder(field.name, className.copy(true)).apply {
                            initializer("null")
                            mutable(true)
                        }.build()
                    )
                }
            }
        } else if (dexBean.nodeType == DexBean.NodeType.INNER) {
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

    private fun getPrimitiveClassName(typeName: String): Pair<TypeName, String> {
        return when (typeName) {
            "boolean" -> {
                Pair(Boolean::class.asTypeName(), "false")
            }
            "char" -> {
                Pair(Char::class.asTypeName(), "'0'")
            }
            "byte" -> {
                Pair(Byte::class.asTypeName(), "0")
            }
            "short" -> {
                Pair(Short::class.asTypeName(), "0")
            }
            "int" -> {
                Pair(Int::class.asTypeName(), "0")
            }
            "float" -> {
                Pair(Float::class.asTypeName(), "0f")
            }
            "long" -> {
                Pair(Long::class.asTypeName(), "0L")
            }
            "double" -> {
                Pair(Double::class.asTypeName(), "0f")
            }
            "void" -> {
                Pair(Void::class.asTypeName(), "null")
            }
            String::class.java.name -> {
                Pair(String::class.asTypeName(), "\"\"")
            }
            else -> {
                Pair(Any::class.asTypeName(), "null")
            }
        }
    }
}