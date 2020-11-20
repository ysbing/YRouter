package com.ysbing.yrouter.core.util

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.ysbing.yrouter.core.DexBean
import com.ysbing.yrouter.core.ExtractDexClassObject
import com.ysbing.yrouter.core.util.DecompileUtil.makeSignature
import com.ysbing.yrouter.core.util.WriteCodeUtil.Companion.getKotlinPackageNameAndClassName
import com.ysbing.yrouter.core.util.WriteCodeUtil.Companion.writeEmptyJava
import jadx.core.codegen.TypeGen
import jadx.core.dex.attributes.AType
import jadx.core.dex.instructions.args.ArgType
import jadx.core.dex.nodes.ClassNode
import org.jetbrains.annotations.Nullable
import java.io.File

/**
 * 将解析后的dex，写入Java文件
 */
class WriteKotlinCodeUtil(
    private val outPath: String,
    private val classNode: ClassNode,
    private val dexBeanList: List<DexBean>
) {
    fun writeKotlin() {
        val saveFile = File(outPath, "main")
        val classBuilder = newClassBuilder(classNode)
        dexBeanList.map {
            beanInfo(classBuilder, it)
        }
        val file = FileSpec.builder(
            classNode.`package`,
            classNode.shortName
        ).addType(classBuilder.build()).build()
        file.writeTo(saveFile)
    }

    private fun newClassBuilder(classNode: ClassNode): TypeSpec.Builder {
        val classType = ExtractDexClassObject.getClassTypeFromClassNode(classNode)
        val classBuilder = when (classType) {
            DexBean.ClassType.OBJECT -> {
                TypeSpec.objectBuilder(classNode.classInfo.shortName)
            }
            DexBean.ClassType.INTERFACE -> {
                TypeSpec.interfaceBuilder(classNode.classInfo.shortName)
            }
            DexBean.ClassType.ENUM -> {
                TypeSpec.enumBuilder(classNode.classInfo.shortName)
            }
            else -> {
                TypeSpec.classBuilder(classNode.classInfo.shortName)
            }
        }
        when {
            classNode.accessFlags.isPublic -> {
                classBuilder.addModifiers(KModifier.PUBLIC)
            }
            classNode.accessFlags.isProtected -> {
                classBuilder.addModifiers(KModifier.PROTECTED)
            }
            classNode.accessFlags.isPrivate -> {
                classBuilder.addModifiers(KModifier.PRIVATE)
            }
        }
        if (classType == DexBean.ClassType.CLASS && classNode.accessFlags.isAbstract) {
            classBuilder.addModifiers(KModifier.ABSTRACT)
        }
        if (classType == DexBean.ClassType.CLASS && !classNode.accessFlags.isAbstract
            && !classNode.accessFlags.isPrivate && !classNode.accessFlags.isFinal
        ) {
            classBuilder.addModifiers(KModifier.OPEN)
        }
        classNode.generics?.map {
            classBuilder.addTypeVariable(TypeVariableName.invoke(it.genericType.toString()))
        }
        if (classType != DexBean.ClassType.ENUM) {
            classNode.interfaces.map {
                val interfaceClassName = getTypeNameFromArgType(it, true)
                classBuilder.addSuperinterface(interfaceClassName)
            }
        }
        if (classType == DexBean.ClassType.CLASS) {
            classNode.superClass?.let {
                if (it.isObject && it.`object` == java.lang.Object::class.java.name) {
                    return@let
                }
                if (classNode.parentClass.fullName == it.`object` && classNode.parentClass.isEnum) {
                    return@let
                }
                classBuilder.superclass(getTypeNameFromArgType(it))
            }
        }
        return classBuilder
    }

    private fun beanInfo(classBuilder: TypeSpec.Builder, dexBean: DexBean) {
        if (dexBean.nodeType == DexBean.NodeType.METHOD) {
            val method = dexBean.method
            classBuilder.addFunction(
                if (method.accessFlags.isConstructor) {
                    if (dexBean.classType == DexBean.ClassType.INTERFACE
                        || dexBean.classType == DexBean.ClassType.OBJECT
                        || dexBean.classNode.shortName == "Companion"
                    ) {
                        return
                    }
                    FunSpec.constructorBuilder()
                } else {
                    FunSpec.builder(method.name)
                }.apply {
                    val methodSignature = method.makeSignature()
                    if (DecompileUtil.methodSupportContains(
                            dexBean.interfaceInfo,
                            methodSignature
                        ) && !method.accessFlags.isConstructor
                    ) {
                        addModifiers(KModifier.OVERRIDE)
                    }
                    if (dexBean.classType == DexBean.ClassType.INTERFACE && method.accessFlags.isAbstract) {
                        addModifiers(KModifier.ABSTRACT)
                    }
                    when {
                        method.accessFlags.isPublic -> {
                            addModifiers(KModifier.PUBLIC)
                        }
                        method.accessFlags.isProtected -> {
                            addModifiers(KModifier.PROTECTED)
                        }
                        method.accessFlags.isPrivate -> {
                            addModifiers(KModifier.PRIVATE)
                        }
                    }
                    if (!method.accessFlags.isConstructor && !method.accessFlags.isPrivate && !method.accessFlags.isFinal) {
                        addModifiers(KModifier.OPEN)
                    }
                    val paramsAnim = method.get(AType.ANNOTATION_MTH_PARAMETERS)?.paramList
                    method.argRegs.mapIndexed argRegs@{ index, arg ->
                        val argType =
                            if (arg.type != ArgType.UNKNOWN) arg.type else arg.initType
                        addParameter(
                            WriteCodeUtil.getSafeMethodName(arg),
                            getTypeNameFromArgType(argType).copy(
                                paramsAnim?.get(index)?.get(Nullable::class.java.name) != null
                            )
                        )
                    }
                    method.generics?.map {
                        addTypeVariable(TypeVariableName(it.genericType.toString()))
                    }
                    if (method.accessFlags.isConstructor) {
                        if (!dexBean.superInfo1.isNullOrEmpty()) {
                            val code = StringBuilder()
                            dexBean.superInfo1.map {
                                val typeName = getTypeNameFromArgType(it)
                                code.append("null as ").append(typeName.toString()).append(",")
                            }
                            code.deleteCharAt(code.length - 1)
                            callSuperConstructor(code.toString())
                        } else if (!dexBean.superInfo2.isNullOrEmpty()) {
                            val code = StringBuilder()
                            dexBean.superInfo2.map {
                                code.append("null as ").append(JavaToKotlinObj.javaToKotlin(it))
                                    .append(",")
                            }
                            code.deleteCharAt(code.length - 1)
                            callSuperConstructor(code.toString())
                        }
                    } else {
                        if (isPrimitive(method.returnType)) {
                            val retClassName =
                                getPrimitiveClassName(method.returnType.toString())
                            if (method.returnType != ArgType.VOID) {
                                if (method.returnType == ArgType.OBJECT) {
                                    returns(Any::class.asClassName())
                                } else {
                                    returns(retClassName)
                                }
                                if (!modifiers.contains(KModifier.ABSTRACT)) {
                                    addStatement(
                                        WriteKotlinMockCodeUtil.mockMethod(
                                            method.parentClass.fullName,
                                            method.name,
                                            retClassName.toString(),
                                            parameters
                                        )
                                    )
                                }
                            } else {
                                if (!modifiers.contains(KModifier.ABSTRACT)) {
                                    addStatement(
                                        WriteKotlinMockCodeUtil.mockVoid(
                                            method.parentClass.fullName,
                                            method.name,
                                            parameters
                                        )
                                    )
                                }
                            }
                        } else {
                            val retClassName = getTypeNameFromArgType(method.returnType).copy(
                                method.getAnnotation(Nullable::class.java.name) != null
                            )
                            returns(retClassName)
                            if (!modifiers.contains(KModifier.ABSTRACT)) {
                                addStatement(
                                    WriteKotlinMockCodeUtil.mockMethod(
                                        method.parentClass.fullName,
                                        method.name,
                                        retClassName.toString(),
                                        parameters
                                    )
                                )
                            }
                        }
                    }
                }.build()
            )
        } else if (dexBean.nodeType == DexBean.NodeType.FIELD) {
            val field = dexBean.field
            val fieldBuilder =
                PropertySpec.builder(
                    field.name,
                    getTypeNameFromArgType(field.type).copy(field.getAnnotation(Nullable::class.java.name) != null)
                ).apply {
                    mutable(!(dexBean.classType == DexBean.ClassType.OBJECT && field.accessFlags.isFinal))
                }
            if (dexBean.interfaceInfo?.contains("${field.fieldInfo.name}:${TypeGen.signature(field.fieldInfo.type)}") == true) {
                fieldBuilder.addModifiers(KModifier.OVERRIDE)
            }
            if (!field.accessFlags.isFinal) {
                fieldBuilder.addModifiers(KModifier.OPEN)
            }
            if (dexBean.classType != DexBean.ClassType.INTERFACE) {
                fieldBuilder.initializer(
                    WriteKotlinMockCodeUtil.mockField(
                        field.parentClass.fullName,
                        field.name,
                        fieldBuilder.build().type.toString()
                    )
                )
            }
            if (dexBean.classType == DexBean.ClassType.ENUM) {
                classBuilder.addEnumConstant(field.name)
            } else {
                classBuilder.addProperty(fieldBuilder.build())
            }
        } else if (dexBean.nodeType == DexBean.NodeType.INNER) {
            val inner = dexBean.inner
            inner.keys.map {
                inner[it]?.groupBy { bean ->
                    bean.classNode
                }?.map inner@{ map ->
                    if (map.key.shortName == "Companion") {
                        //空内部类不做写入
                        if (map.value.size == 1 && map.value[0].nodeType == DexBean.NodeType.INNER && map.value[0].inner.isEmpty()) {
                            return@inner
                        }
                        val innerClassBuilder = TypeSpec.companionObjectBuilder()
                        map.value.map { bean ->
                            beanInfo(innerClassBuilder, bean)
                        }
                        classBuilder.addType(innerClassBuilder.build())
                    } else {
                        val innerClassBuilder = newClassBuilder(map.key)
                        map.value.map { bean ->
                            beanInfo(innerClassBuilder, bean)
                        }
                        classBuilder.addType(innerClassBuilder.build())
                    }
                }
            }
        }
    }

    private fun getPrimitiveClassName(typeName: String): TypeName {
        return JavaToKotlinObj.getKotlinPrimitiveType(typeName).asClassName()
    }

    private fun isPrimitive(arg: ArgType): Boolean {
        if (arg.isPrimitive) {
            return true
        }
        if (arg.isObject) {
            val primitiveType = getPrimitiveClassName(arg.`object`)
            if (primitiveType != Any::class.asClassName()) {
                return true
            }
        }
        return false
    }

    private fun getClassName(
        packageName: String,
        className: String,
        isInterface: Boolean = false,
        genericCount: Int = 0
    ): ClassName {
        return ClassName(packageName, className.split("$")).apply {
            writeEmptyJava(outPath, this.packageName, this.simpleNames, isInterface, genericCount)
        }
    }

    private fun getTypeNameFromArgType(argType: ArgType, isInterface: Boolean = false): TypeName {
        return when {
            isPrimitive(argType) -> {
                getPrimitiveClassName(argType.toString())
            }
            argType.wildcardType != null -> {
                getWildcardTypeName(argType)
            }
            argType.isGenericType -> {
                val genericTypeNames = getKotlinPackageNameAndClassName(argType.`object`)
                getClassName(genericTypeNames[0], genericTypeNames[1])
            }
            argType.isGeneric -> {
                getGenericTypeName(argType, isInterface)
            }
            argType.isArray -> {
                getArrayTypeName(argType.arrayElement)
            }
            else -> {
                val names = getKotlinPackageNameAndClassName(argType.`object`)
                getClassName(names[0], names[1], isInterface)
            }
        }
    }

    private fun getGenericTypeName(argType: ArgType, isInterface: Boolean = false): TypeName {
        if (argType.innerType != null) {
            return getTypeNameFromArgType(argType.innerType)
        }
        val names = getKotlinPackageNameAndClassName(argType.`object`)
        val className =
            getClassName(
                names[0],
                names[1],
                isInterface,
                argType.genericTypes?.size ?: 0
            )
        val typeNames = ArrayList<TypeName>()
        argType.genericTypes?.map { genericType ->
            typeNames.add(getTypeNameFromArgType(genericType))
        }
        return className.parameterizedBy(typeNames)
    }

    private fun getArrayTypeName(argType: ArgType): TypeName {
        return if (argType.isArray) {
            getArrayTypeName(argType.arrayElement)
        } else {
            var typeName: TypeName? = null
            if (argType.isPrimitive) {
                typeName = JavaToKotlinObj.getPrimitiveArrayType(argType.toString())?.asTypeName()
            }
            if (typeName == null) {
                val array = Array::class.asClassName()
                array.parameterizedBy(getTypeNameFromArgType(argType))
            } else {
                typeName
            }
        }
    }

    private fun getWildcardTypeName(argType: ArgType): TypeName {
        return when (argType.wildcardBound) {
            ArgType.WildcardBound.EXTENDS -> {
                WildcardTypeName.producerOf(getTypeNameFromArgType(argType.wildcardType))
            }
            ArgType.WildcardBound.SUPER -> {
                WildcardTypeName.consumerOf(getTypeNameFromArgType(argType.wildcardType))
            }
            ArgType.WildcardBound.UNBOUND -> {
                STAR
            }
            else -> {
                getTypeNameFromArgType(argType.wildcardType)
            }
        }
    }
}
