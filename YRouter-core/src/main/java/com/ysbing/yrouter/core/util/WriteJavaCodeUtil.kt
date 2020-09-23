package com.ysbing.yrouter.core.util

import com.squareup.javapoet.*
import com.squareup.kotlinpoet.asTypeName
import com.ysbing.yrouter.core.DexBean
import com.ysbing.yrouter.core.ExtractDexClass
import com.ysbing.yrouter.core.util.WriteCodeUtil.getPackageNameAndClassName
import com.ysbing.yrouter.core.util.WriteCodeUtil.writeEmptyJava
import jadx.core.dex.instructions.args.ArgType
import jadx.core.dex.nodes.ClassNode
import java.io.File
import java.io.IOException
import java.lang.reflect.Type
import javax.lang.model.element.Modifier

/**
 * 将解析后的dex，写入Java文件
 */
object WriteJavaCodeUtil {
    fun writeJava(outPath: String, classNode: ClassNode, dexBeanList: List<DexBean>) {
        val saveFile = File(outPath, "main")
        val classBuilder = newClassBuilder(classNode, outPath)
        for (dexBean in dexBeanList) {
            beanInfo(classBuilder, dexBean, outPath)
        }
        val file = JavaFile.builder(
            classNode.getPackage(),
            classBuilder.build()
        ).build()
        try {
            file.writeTo(saveFile)
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    private fun newClassBuilder(classNode: ClassNode, outPath: String): TypeSpec.Builder {
        val classType = ExtractDexClass.getClassTypeFromClassNode(classNode)
        val classBuilder = when (classType) {
            DexBean.ClassType.INTERFACE -> {
                TypeSpec.interfaceBuilder(classNode.classInfo.shortName)
            }
            else -> {
                TypeSpec.classBuilder(classNode.classInfo.shortName)
            }
        }
        if (classNode.accessFlags.isPublic) {
            classBuilder.addModifiers(Modifier.PUBLIC)
        }
        if (classNode.accessFlags.isProtected) {
            classBuilder.addModifiers(Modifier.PROTECTED)
        }
        if (classType != DexBean.ClassType.INTERFACE && classNode.accessFlags.isAbstract) {
            classBuilder.addModifiers(Modifier.ABSTRACT)
        }
        if (classNode.accessFlags.isStatic) {
            classBuilder.addModifiers(Modifier.STATIC)
        }
        if (classNode.accessFlags.isFinal) {
            classBuilder.addModifiers(Modifier.FINAL)
        }
        val superClass = classNode.superClass
        if (superClass != null && classType == DexBean.ClassType.CLASS) {
            val names = getPackageNameAndClassName(superClass.getObject())
            classBuilder.superclass(ClassName.get(names[0], names[1]))
            writeEmptyJava(outPath, names[0], names[1])
        }
        return classBuilder
    }

    private fun beanInfo(classBuilder: TypeSpec.Builder, dexBean: DexBean, outPath: String) {
        if (dexBean.nodeType == DexBean.NodeType.METHOD) {
            val method = dexBean.method
            val methodBuilder = MethodSpec.methodBuilder(method.name)
            methodBuilder.addModifiers(Modifier.PUBLIC)
            if (method.accessFlags.isPublic) {
                methodBuilder.addModifiers(Modifier.PUBLIC)
            }
            if (method.accessFlags.isProtected) {
                methodBuilder.addModifiers(Modifier.PROTECTED)
            }
            if (method.accessFlags.isStatic) {
                methodBuilder.addModifiers(Modifier.STATIC)
            }
            if (method.accessFlags.isFinal) {
                methodBuilder.addModifiers(Modifier.FINAL)
            }
            for (arg in method.argRegs) {
                if (arg.type.isPrimitive) {
                    methodBuilder.addParameter(
                        getPrimitiveClassName(arg.type.toString()).first,
                        WriteCodeUtil.getSafeMethodName(arg)
                    )
                } else {
                    val argType =
                        if (arg.type != ArgType.UNKNOWN) arg.type else arg.initType
                    val names = getPackageNameAndClassName(argType.getObject())
                    writeEmptyJava(outPath, names[0], names[1])
                    val className = ClassName.get(names[0], names[1])
                    if (argType.isGeneric) {
                        val typeNames = arrayOfNulls<ClassName>(argType.genericTypes.size)
                        for (i in argType.genericTypes.indices) {
                            val genericType = argType.genericTypes[i]
                            val genericTypeNames =
                                getPackageNameAndClassName(genericType.getObject())
                            typeNames[i] = ClassName.get(genericTypeNames[0], genericTypeNames[1])
                            writeEmptyJava(outPath, genericTypeNames[0], genericTypeNames[1])
                        }
                        methodBuilder.addParameter(
                            ParameterizedTypeName.get(className, *typeNames),
                            WriteCodeUtil.getSafeMethodName(arg)
                        )
                    } else {
                        methodBuilder.addParameter(
                            className,
                            WriteCodeUtil.getSafeMethodName(arg)
                        )
                    }
                }
            }
            if (method.returnType != null) {
                if (method.returnType.isPrimitive) {
                    val (first, second) = getPrimitiveClassName(method.returnType.toString())
                    if (first != Void::class.java) {
                        methodBuilder.returns(first)
                        methodBuilder.addStatement("return $second")
                    }
                } else {
                    val names = getPackageNameAndClassName(method.returnType.getObject())
                    methodBuilder.returns(ClassName.get(names[0], names[1]))
                    writeEmptyJava(outPath, names[0], names[1])
                    methodBuilder.addStatement("return null")
                }
            }
            classBuilder.addMethod(methodBuilder.build())
        } else if (dexBean.nodeType == DexBean.NodeType.FIELD) {
            val field = dexBean.field
            val fieldBuilder: FieldSpec.Builder
            if (field.type.isPrimitive) {
                val (first, second) = getPrimitiveClassName(field.type.toString())
                fieldBuilder = FieldSpec.builder(first, field.name)
                fieldBuilder.initializer(second)
            } else {
                val names = getPackageNameAndClassName(field.type.getObject())
                writeEmptyJava(outPath, names[0], names[1])
                val className = ClassName.get(names[0], names[1])
                if (field.type.isGeneric) {
                    val typeNames = arrayOfNulls<ClassName>(field.type.genericTypes.size)
                    for (i in field.type.genericTypes.indices) {
                        val genericType = field.type.genericTypes[i]
                        val genericTypeNames = getPackageNameAndClassName(genericType.getObject())
                        typeNames[i] = ClassName.get(genericTypeNames[0], genericTypeNames[1])
                        writeEmptyJava(outPath, genericTypeNames[0], genericTypeNames[1])
                    }
                    fieldBuilder = FieldSpec.builder(
                        ParameterizedTypeName.get(className, *typeNames),
                        field.name
                    )
                } else {
                    fieldBuilder = FieldSpec.builder(className, field.name)
                }
                fieldBuilder.initializer("null")
            }
            if (field.accessFlags.isPublic) {
                fieldBuilder.addModifiers(Modifier.PUBLIC)
            }
            if (field.accessFlags.isProtected) {
                fieldBuilder.addModifiers(Modifier.PROTECTED)
            }
            if (field.accessFlags.isStatic) {
                fieldBuilder.addModifiers(Modifier.STATIC)
            }
            if (field.accessFlags.isFinal) {
                fieldBuilder.addModifiers(Modifier.FINAL)
            }
            classBuilder.addField(fieldBuilder.build())
        } else if (dexBean.nodeType == DexBean.NodeType.INNER) {
            val inner = dexBean.inner
            inner.keys.map {
                inner[it]?.groupBy { bean ->
                    bean.classNode
                }?.map { map ->
                    val innerClassBuilder = newClassBuilder(map.key, outPath)
                    map.value.map { bean ->
                        beanInfo(innerClassBuilder, bean, outPath)
                    }
                    classBuilder.addType(innerClassBuilder.build())
                }
            }
        }
    }

    private fun getPrimitiveClassName(typeName: String): Pair<Type, String> {
        return when (typeName) {
            "boolean" -> {
                Pair(Boolean::class.java, "false")
            }
            "char" -> {
                Pair(Char::class.java, "'0'")
            }
            "byte" -> {
                Pair(Byte::class.java, "0")
            }
            "short" -> {
                Pair(Short::class.java, "0")
            }
            "int" -> {
                Pair(Int::class.java, "0")
            }
            "float" -> {
                Pair(Float::class.java, "0f")
            }
            "long" -> {
                Pair(Long::class.java, "0L")
            }
            "double" -> {
                Pair(Double::class.java, "0f")
            }
            "void" -> {
                Pair(Void::class.java, "null")
            }
            String::class.java.name -> {
                Pair(String::class.java, "\"\"")
            }
            else -> {
                Pair(Any::class.java, "null")
            }
        }
    }
}