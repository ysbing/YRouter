package com.ysbing.yrouter.core.util

import com.squareup.javapoet.*
import com.ysbing.yrouter.core.DexBean
import com.ysbing.yrouter.core.ExtractDexClassObject
import com.ysbing.yrouter.core.util.WriteCodeUtil.Companion.getPackageNameAndClassName
import com.ysbing.yrouter.core.util.WriteCodeUtil.Companion.writeEmptyJava
import jadx.core.dex.instructions.args.ArgType
import jadx.core.dex.nodes.ClassNode
import java.io.File
import javax.lang.model.element.Modifier

/**
 * 将解析后的dex，写入Java文件
 */
class WriteJavaCodeUtil(
    private val outPath: String,
    private val classNode: ClassNode,
    private val dexBeanList: List<DexBean>
) {
    fun writeJava() {
        val saveFile = File(outPath, "main")
        val classBuilder = newClassBuilder(classNode)
        for (dexBean in dexBeanList) {
            beanInfo(classBuilder, dexBean, outPath)
        }
        //空枚举类JavaPoet写入会抛异常
        if (classNode.isEnum && classBuilder.enumConstants.isEmpty()) {
            return
        }
        val file = JavaFile.builder(
            classNode.getPackage(),
            classBuilder.build()
        ).build()
        file.writeTo(saveFile)
    }

    private fun newClassBuilder(
        classNode: ClassNode,
        isInner: Boolean = false
    ): TypeSpec.Builder {
        val classType = ExtractDexClassObject.getClassTypeFromClassNode(classNode)
        val classBuilder = when (classType) {
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
                classBuilder.addModifiers(Modifier.PUBLIC)
            }
            classNode.accessFlags.isProtected -> {
                classBuilder.addModifiers(Modifier.PROTECTED)
            }
            classNode.accessFlags.isPrivate -> {
                classBuilder.addModifiers(Modifier.PRIVATE)
            }
        }
        if (classType == DexBean.ClassType.CLASS && classNode.accessFlags.isAbstract) {
            classBuilder.addModifiers(Modifier.ABSTRACT)
        }
        if (classType != DexBean.ClassType.ENUM && classNode.accessFlags.isFinal) {
            classBuilder.addModifiers(Modifier.FINAL)
        }
        if (isInner && classNode.accessFlags.isStatic) {
            classBuilder.addModifiers(Modifier.STATIC)
        }
        if (classNode.accessFlags.isNative) {
            classBuilder.addModifiers(Modifier.NATIVE)
        }
        classNode.generics?.map {
            classBuilder.addTypeVariable(TypeVariableName.get(it.genericType.toString()))
        }
        if (classType != DexBean.ClassType.ENUM) {
            classNode.interfaces.map {
                val interfaceClassName = getTypeNameFromArgType(it, true)
                classBuilder.addSuperinterface(interfaceClassName)
            }
        }
        val superClass = classNode.superClass
        if (superClass != null && classType == DexBean.ClassType.CLASS) {
            classBuilder.superclass(getTypeNameFromArgType(superClass))
        }
        return classBuilder
    }

    private fun beanInfo(classBuilder: TypeSpec.Builder, dexBean: DexBean, outPath: String) {
        if (dexBean.nodeType == DexBean.NodeType.METHOD) {
            val method = dexBean.method
            val methodBuilder =
                if (method.accessFlags.isConstructor) {
                    if (dexBean.classType == DexBean.ClassType.INTERFACE) {
                        return
                    }
                    MethodSpec.constructorBuilder()
                } else {
                    MethodSpec.methodBuilder(method.name)
                }
            when {
                method.accessFlags.isPublic -> {
                    methodBuilder.addModifiers(Modifier.PUBLIC)
                }
                method.accessFlags.isProtected -> {
                    methodBuilder.addModifiers(Modifier.PROTECTED)
                }
                method.accessFlags.isPrivate -> {
                    methodBuilder.addModifiers(Modifier.PRIVATE)
                }
            }
            if (method.accessFlags.isStatic && !method.accessFlags.isConstructor) {
                methodBuilder.addModifiers(Modifier.STATIC)
            }
            if (method.accessFlags.isFinal) {
                methodBuilder.addModifiers(Modifier.FINAL)
            }
            if (dexBean.classNode.accessFlags.isAbstract && method.accessFlags.isAbstract) {
                methodBuilder.addModifiers(Modifier.ABSTRACT)
            }
            methodBuilder.modifiers.contains(Modifier.ABSTRACT)
            if (dexBean.classNode.accessFlags.isInterface &&
                !(method.accessFlags.isAbstract || method.accessFlags.isStatic)
            ) {
                methodBuilder.addModifiers(Modifier.DEFAULT)
            }
            for (arg in method.argRegs) {
                val argType =
                    if (arg.type != ArgType.UNKNOWN) arg.type else arg.initType
                methodBuilder.addParameter(
                    getTypeNameFromArgType(argType),
                    WriteCodeUtil.getSafeMethodName(arg)
                )
            }
            method.generics?.map {
                methodBuilder.addTypeVariable(TypeVariableName.get(it.genericType.toString()))
            }
            if (method.accessFlags.isConstructor) {
                if (!dexBean.superInfo1.isNullOrEmpty()) {
                    val code = StringBuilder()
                    code.append("super(")
                    dexBean.superInfo1.map {
                        val typeName = getTypeNameFromArgType(it)
                        code.append("(").append(typeName.toString())
                            .append(")(java.lang.Object)null,")
                    }
                    code.deleteCharAt(code.length - 1)
                    code.append(")")
                    methodBuilder.addStatement(code.toString().replace("$", "$$"))
                } else if (!dexBean.superInfo2.isNullOrEmpty()) {
                    val code = StringBuilder()
                    code.append("super(")
                    dexBean.superInfo2.map {
                        code.append("(").append(it).append(")(java.lang.Object)null,")
                    }
                    code.deleteCharAt(code.length - 1)
                    code.append(")")
                    methodBuilder.addStatement(code.toString().replace("$", "$$"))
                }
            } else {
                if (isPrimitive(method.returnType)) {
                    val retClassName = getPrimitiveClassName(method.returnType.toString())
                    if (method.returnType != ArgType.VOID) {
                        methodBuilder.returns(retClassName)
                        if (!methodBuilder.modifiers.contains(Modifier.ABSTRACT)) {
                            methodBuilder.addStatement(
                                WriteJavaMockCodeUtil.mockMethod(
                                    method.parentClass.fullName,
                                    method.name,
                                    retClassName.toString(),
                                    methodBuilder.parameters
                                )
                            )
                        }
                    } else {
                        if (!methodBuilder.modifiers.contains(Modifier.ABSTRACT)) {
                            methodBuilder.addStatement(
                                WriteJavaMockCodeUtil.mockVoid(
                                    method.parentClass.fullName,
                                    method.name, methodBuilder.parameters
                                )
                            )
                        }
                    }
                } else {
                    val retClassName = getTypeNameFromArgType(method.returnType)
                    methodBuilder.returns(retClassName)
                    if (!methodBuilder.modifiers.contains(Modifier.ABSTRACT)) {
                        methodBuilder.addStatement(
                            WriteJavaMockCodeUtil.mockMethod(
                                method.parentClass.fullName,
                                method.name,
                                retClassName.toString(),
                                methodBuilder.parameters
                            )
                        )
                    }
                }
            }
            classBuilder.addMethod(methodBuilder.build())
        } else if (dexBean.nodeType == DexBean.NodeType.FIELD) {
            val field = dexBean.field
            val fieldBuilder: FieldSpec.Builder
            fieldBuilder = FieldSpec.builder(getTypeNameFromArgType(field.type), field.name)
            fieldBuilder.initializer(
                WriteJavaMockCodeUtil.mockField(
                    field.parentClass.fullName,
                    field.name,
                    fieldBuilder.build().type.toString()
                )
            )
            when {
                field.accessFlags.isPublic -> {
                    fieldBuilder.addModifiers(Modifier.PUBLIC)
                }
                field.accessFlags.isProtected -> {
                    fieldBuilder.addModifiers(Modifier.PROTECTED)
                }
                field.accessFlags.isPrivate -> {
                    fieldBuilder.addModifiers(Modifier.PRIVATE)
                }
            }
            //内部类不能具有静态声明
            if (dexBean.classType != DexBean.ClassType.CLASS
                || field.parentClass.parentClass == null
                && field.accessFlags.isStatic
            ) {
                fieldBuilder.addModifiers(Modifier.STATIC)
            }
            if (field.accessFlags.isFinal) {
                fieldBuilder.addModifiers(Modifier.FINAL)
            }
            if (dexBean.classType == DexBean.ClassType.ENUM) {
                classBuilder.addEnumConstant(field.name)
            } else {
                classBuilder.addField(fieldBuilder.build())
            }
        } else if (dexBean.nodeType == DexBean.NodeType.INNER) {
            val inner = dexBean.inner
            inner.keys.map {
                inner[it]?.groupBy { bean ->
                    bean.classNode
                }?.map inner@{ map ->
                    val innerClassBuilder = newClassBuilder(map.key, true)
                    map.value.map { bean ->
                        beanInfo(innerClassBuilder, bean, outPath)
                    }
                    if (!map.key.isEnum || innerClassBuilder.enumConstants.isNotEmpty()) {
                        classBuilder.addType(innerClassBuilder.build())
                    }
                }
            }
        }
    }

    private fun getPrimitiveClassName(typeName: String): TypeName {
        val type = JavaToKotlinObj.getPrimitiveType(typeName)
        return if (type.second) {
            TypeName.get(type.first).box()
        } else {
            TypeName.get(type.first)
        }
    }

    private fun isPrimitive(arg: ArgType): Boolean {
        if (arg.isPrimitive) {
            return true
        }
        if (arg.isObject) {
            val primitiveType = JavaToKotlinObj.getPrimitiveType(arg.`object`)
            if (primitiveType.first != Any::class.java) {
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
        val classNames = className.split("$")
        return if (classNames.size > 1) {
            ClassName.get(
                packageName,
                classNames[0],
                *classNames.subList(1, classNames.size).toTypedArray()
            )
        } else {
            ClassName.get(packageName, classNames[0])
        }.apply {
            writeEmptyJava(
                outPath,
                this.packageName(),
                this.simpleNames(),
                isInterface,
                genericCount
            )
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
                val genericTypeNames = getPackageNameAndClassName(argType.`object`)
                return getClassName(genericTypeNames[0], genericTypeNames[1])
            }
            argType.isGeneric -> {
                getGenericTypeName(argType, isInterface)
            }
            argType.isArray -> {
                getArrayTypeName(argType.arrayElement)
            }
            else -> {
                val names = getPackageNameAndClassName(argType.`object`)
                getClassName(names[0], names[1], isInterface)
            }
        }
    }

    private fun getGenericTypeName(argType: ArgType, isInterface: Boolean = false): TypeName {
        if (argType.innerType != null) {
            return getTypeNameFromArgType(argType.innerType)
        }
        val names = getPackageNameAndClassName(argType.`object`)
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
        return ParameterizedTypeName.get(className, *typeNames.toTypedArray())
    }

    private fun getArrayTypeName(argType: ArgType): TypeName {
        return if (isPrimitive(argType)) {
            val pair = getPrimitiveClassName(argType.toString())
            ArrayTypeName.of(pair)
        } else {
            if (argType.isArray) {
                getArrayTypeName(argType.arrayElement)
            } else {
                val names =
                    getPackageNameAndClassName(argType.`object`)
                ArrayTypeName.of(getClassName(names[0], names[1]))
            }
        }
    }

    private fun getWildcardTypeName(argType: ArgType): TypeName {
        val typeName = getTypeNameFromArgType(argType.wildcardType).box()
        typeName.isPrimitive
        return when (argType.wildcardBound) {
            ArgType.WildcardBound.EXTENDS -> {
                WildcardTypeName.subtypeOf(typeName)
            }
            ArgType.WildcardBound.SUPER -> {
                WildcardTypeName.supertypeOf(typeName)
            }
            ArgType.WildcardBound.UNBOUND -> {
                WildcardTypeName.subtypeOf(TypeName.OBJECT)
            }
            else -> {
                typeName
            }
        }
    }
}