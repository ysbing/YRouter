package com.ysbing.yrouter.core.util

import com.squareup.javapoet.*
import com.ysbing.yrouter.api.mock.YRouterMockPair
import java.io.File
import javax.lang.model.element.Modifier

/**
 * 模拟java代码
 */
object WriteJavaMockCodeUtil {

    const val CLASS_PACKAGE = "com.ysbing.yrouter.mock"
    const val CLASS_NAME = "YRouterMock"
    const val METHOD_MOCK = "methodMock"
    const val METHOD_VOID_MOCK = "methodVoidMock"
    const val FIELD_MOCK = "fieldMock"
    const val CLASS_NAME_MOCK_CONFIG = "YRouterMockConfig"
    const val FIELD_MOCK_CONFIG = "config"

    fun writeEmptyMock(outPath: String) {
        val saveFile = File(outPath, "lib")
        val classBuilder = TypeSpec.classBuilder(CLASS_NAME)
        classBuilder.addModifiers(Modifier.PUBLIC)
        val mockPairClassName = YRouterMockPair::class.java

        val fieldBuilder = MethodSpec.methodBuilder(FIELD_MOCK)
        fieldBuilder.addParameter(String::class.java, "className")
        fieldBuilder.addParameter(String::class.java, "name")
        fieldBuilder.addParameter(String::class.java, "retType")
        fieldBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        fieldBuilder.returns(Any::class.java)
        fieldBuilder.addStatement("return null")

        val methodBuilder = MethodSpec.methodBuilder(METHOD_MOCK)
        methodBuilder.addParameter(String::class.java, "className")
        methodBuilder.addParameter(String::class.java, "name")
        methodBuilder.addParameter(String::class.java, "retType")
        methodBuilder.addParameter(ArrayTypeName.of(mockPairClassName), "args").varargs()
        methodBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC)
        methodBuilder.returns(Any::class.java)
        methodBuilder.addStatement("return null")

        val voidMethodBuilder = MethodSpec.methodBuilder(METHOD_VOID_MOCK)
        voidMethodBuilder.addParameter(String::class.java, "className")
        voidMethodBuilder.addParameter(String::class.java, "name")
        voidMethodBuilder.addParameter(ArrayTypeName.of(mockPairClassName), "args").varargs()
        voidMethodBuilder.addModifiers(Modifier.PUBLIC, Modifier.STATIC)

        classBuilder.addMethod(fieldBuilder.build())
        classBuilder.addMethod(methodBuilder.build())
        classBuilder.addMethod(voidMethodBuilder.build())

        val file = JavaFile.builder(
            CLASS_PACKAGE,
            classBuilder.build()
        ).build()
        file.writeTo(saveFile)

        val pairClassBuilder = TypeSpec.classBuilder(mockPairClassName.simpleName)
        pairClassBuilder.addModifiers(Modifier.PUBLIC)
        pairClassBuilder.addMethod(
            MethodSpec.constructorBuilder()
                .addParameter(String::class.java, "className")
                .addParameter(Any::class.java, "args")
                .addModifiers(Modifier.PUBLIC)
                .build()
        )
        val pairClassFile = JavaFile.builder(
            mockPairClassName.`package`.name,
            pairClassBuilder.build()
        ).build()
        pairClassFile.writeTo(saveFile)
    }

    fun mockField(
        className: String,
        name: String,
        retType: String
    ): String {
        return "($retType)$CLASS_PACKAGE.$CLASS_NAME.$FIELD_MOCK(\"$className\",\"$name\",\"$retType\")"
    }

    fun mockMethod(
        className: String,
        name: String,
        retType: String,
        args: List<ParameterSpec>
    ): String {
        return if (args.isNotEmpty()) {
            "return ($retType)$CLASS_PACKAGE.$CLASS_NAME.$METHOD_MOCK(" +
                    "\"$className\",\"$name\",\"$retType\",${getArgs(args)})"
        } else {
            "return ($retType)$CLASS_PACKAGE.$CLASS_NAME.$METHOD_MOCK(" +
                    "\"$className\",\"$name\",\"$retType\")"
        }
    }

    fun mockVoid(
        className: String,
        name: String,
        args: List<ParameterSpec>
    ): String {
        return if (args.isNotEmpty()) {
            "$CLASS_PACKAGE.$CLASS_NAME.$METHOD_VOID_MOCK(\"$className\",\"$name\",${
            getArgs(args)})"
        } else {
            "$CLASS_PACKAGE.$CLASS_NAME.$METHOD_VOID_MOCK(\"$className\",\"$name\")"
        }
    }

    private fun getArgs(args: List<ParameterSpec>): String {
        if (args.isEmpty()) {
            return ""
        }
        val str = StringBuilder()
        args.map {
            str.append("\nnew ${YRouterMockPair::class.java.name}(\n\"")
            str.append(it.type).append("\",").append(it.name)
            str.append("),")
        }
        str.deleteAt(str.length - 1)
        return str.toString()
    }
}