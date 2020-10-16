package com.ysbing.yrouter.core.util

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.ysbing.yrouter.core.DexBean
import com.ysbing.yrouter.core.ExtractDexClassObject
import com.ysbing.yrouter.core.util.WriteCodeUtil.Companion.getPackageNameAndClassName
import com.ysbing.yrouter.core.util.WriteCodeUtil.Companion.writeEmptyJava
import jadx.core.dex.instructions.args.ArgType
import jadx.core.dex.nodes.ClassNode
import java.io.File

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
        val classType = ExtractDexClassObject.getClassTypeFromClassNode(classNode)
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
                        if (isPrimitive(arg.type)) {
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
                                    if (isPrimitive(argType.arrayElement)) {
                                        val pair =
                                            getPrimitiveClassName(argType.arrayElement.toString())
                                        val array = Array::class.asClassName()
                                        array.parameterizedBy(pair.first)
                                    } else {
                                        val names =
                                            getPackageNameAndClassName(argType.arrayElement.`object`)
                                        val array = Array::class.asClassName()
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
                        if (isPrimitive(method.returnType)) {
                            val retClassName = getPrimitiveClassName(method.returnType.toString())
                            if (retClassName.first != Void::class.asTypeName()) {
                                if (retClassName.first == Any::class.asClassName()) {
                                    returns(Any::class.asClassName().copy(true))
                                } else {
                                    returns(retClassName.first)
                                }
                                addStatement(
                                    WriteKotlinMockCodeUtil.mockMethod(
                                        method.parentClass.fullName,
                                        method.name,
                                        retClassName.first,
                                        parameters
                                    )
                                )
                            } else {
                                addStatement(
                                    WriteKotlinMockCodeUtil.mockVoid(
                                        method.parentClass.fullName,
                                        method.name,
                                        parameters
                                    )
                                )
                            }
                        } else {
                            val names = getPackageNameAndClassName(method.returnType.`object`)
                            val retClassName = ClassName(names[0], names[1]).apply {
                                writeEmptyJava(outPath, packageName, simpleName)
                            }
                            returns(retClassName.copy(true))
                            addStatement(
                                WriteKotlinMockCodeUtil.mockMethod(
                                    method.parentClass.fullName,
                                    method.name,
                                    retClassName,
                                    parameters
                                )
                            )
                        }
                    }
                }.build()
            )
        } else if (dexBean.nodeType == DexBean.NodeType.FIELD) {
            val field = dexBean.field
            val fieldBuilder: PropertySpec.Builder
            if (field.type.isObject && field.type.`object` == String::class.java.name || isPrimitive(
                    field.type
                )
            ) {
                val pair = getPrimitiveClassName(field.type.toString())
                fieldBuilder =
                    PropertySpec.builder(
                        field.name,
                        pair.first
                    ).apply {
                        val classType =
                            ExtractDexClassObject.getClassTypeFromClassNode(field.parentClass)
                        mutable(!(classType == DexBean.ClassType.OBJECT && field.accessFlags.isFinal))
                    }
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
                    fieldBuilder =
                        PropertySpec.builder(
                            field.name,
                            className.parameterizedBy(typeNames).copy(true)
                        ).apply {
                            mutable(true)
                        }
                } else {
                    val className = if (field.type.isArray) {
                        if (isPrimitive(field.type.arrayElement)) {
                            val pair = getPrimitiveClassName(field.type.arrayElement.toString())
                            val array = Array::class.asClassName()
                            array.parameterizedBy(pair.first)
                        } else {
                            val names =
                                getPackageNameAndClassName(field.type.arrayElement.`object`)
                            val array = Array::class.asClassName()
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
                    fieldBuilder =
                        PropertySpec.builder(field.name, className.copy(true)).apply {
                            mutable(true)
                        }
                }
            }
            fieldBuilder.initializer(
                WriteKotlinMockCodeUtil.mockField(
                    field.parentClass.fullName,
                    field.name,
                    fieldBuilder.build().type as ClassName
                )
            )
            classBuilder.addProperty(fieldBuilder.build())
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

    private fun getPrimitiveClassName(typeName: String): Pair<ClassName, String> {
        return when (typeName) {
            "boolean" -> {
                Pair(Boolean::class.asClassName(), "false")
            }
            "char" -> {
                Pair(Char::class.asClassName(), "'0'")
            }
            "byte" -> {
                Pair(Byte::class.asClassName(), "0")
            }
            "short" -> {
                Pair(Short::class.asClassName(), "0")
            }
            "int" -> {
                Pair(Int::class.asClassName(), "0")
            }
            "float" -> {
                Pair(Float::class.asClassName(), "0f")
            }
            "long" -> {
                Pair(Long::class.asClassName(), "0L")
            }
            "double" -> {
                Pair(Double::class.asClassName(), "0f")
            }
            "void" -> {
                Pair(Void::class.asClassName(), "null")
            }
            String::class.java.name -> {
                Pair(String::class.asClassName(), "\"\"")
            }
            else -> {
                Pair(Any::class.asClassName(), "null")
            }
        }
    }

    private fun isPrimitive(arg: ArgType): Boolean {
        if (arg.isPrimitive) {
            return true
        }
        if (arg.isObject) {
            val primitiveType = getPrimitiveClassName(arg.`object`)
            if (primitiveType.first != Any::class.asClassName()) {
                return true
            }
        }
        return false
    }
}