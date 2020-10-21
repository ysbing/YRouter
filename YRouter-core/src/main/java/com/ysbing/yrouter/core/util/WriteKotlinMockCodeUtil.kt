package com.ysbing.yrouter.core.util

import com.squareup.kotlinpoet.*
import com.ysbing.yrouter.api.mock.YRouterMockPair
import com.ysbing.yrouter.core.util.WriteJavaMockCodeUtil.CLASS_NAME
import com.ysbing.yrouter.core.util.WriteJavaMockCodeUtil.CLASS_PACKAGE
import com.ysbing.yrouter.core.util.WriteJavaMockCodeUtil.FIELD_MOCK
import com.ysbing.yrouter.core.util.WriteJavaMockCodeUtil.METHOD_MOCK
import com.ysbing.yrouter.core.util.WriteJavaMockCodeUtil.METHOD_VOID_MOCK
import java.io.File

object WriteKotlinMockCodeUtil {

    fun writeMockConfigJava(saveFile: File, config: String) {
        val classBuilder = TypeSpec.objectBuilder(WriteJavaMockCodeUtil.CLASS_NAME_MOCK_CONFIG)
        classBuilder.addProperty(
            PropertySpec.builder(WriteJavaMockCodeUtil.FIELD_MOCK_CONFIG, String::class)
                .addModifiers(KModifier.CONST)
                .initializer("\"\"\"$config\"\"\"").build()
        )
        val javaFile = FileSpec.builder(CLASS_PACKAGE, WriteJavaMockCodeUtil.CLASS_NAME_MOCK_CONFIG)
            .addType(classBuilder.build()).build()
        javaFile.writeTo(saveFile)
    }

    fun mockField(className: String, name: String, retType: ClassName): String {
        return "$CLASS_PACKAGE.$CLASS_NAME.$FIELD_MOCK(\"$className\",\"$name\",\"${retType.simpleName.toLowerCase()}\") as $retType"
    }

    fun mockMethod(
        className: String,
        name: String,
        retType: ClassName,
        args: List<ParameterSpec>
    ): String {
        return if (args.isNotEmpty()) {
            "return $CLASS_PACKAGE.$CLASS_NAME.$METHOD_MOCK(\"$className\",\"$name\"," +
                    "\"${retType.simpleName.toLowerCase()}\",${getArgs(args)}) as $retType"
        } else {
            "return $CLASS_PACKAGE.$CLASS_NAME.$METHOD_MOCK(\"$className\",\"$name\"," +
                    "\"${retType.simpleName.toLowerCase()}\") as $retType"
        }
    }

    fun mockVoid(
        className: String,
        name: String,
        args: List<ParameterSpec>
    ): String {
        return if (args.isNotEmpty()) {
            "$CLASS_PACKAGE.$CLASS_NAME.$METHOD_VOID_MOCK(\"$className\",\"$name\",${getArgs(args)})"
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
            str.append("\n${YRouterMockPair::class.java.name}(\n\"")
            str.append(it.type).append("\",").append(it.name)
            str.append("),")
        }
        str.deleteAt(str.length - 1)
        return str.toString()
    }
}