package com.ysbing.yrouter.core.util

import com.squareup.javapoet.ParameterSpec
import com.ysbing.yrouter.api.mock.YRouterMockPair

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

    fun mockField(
        className: String,
        name: String,
        retType: String
    ): String {
        return "($retType)$CLASS_PACKAGE.$CLASS_NAME.INSTANCE.$FIELD_MOCK(\"$className\",\"$name\",\"$retType\")"
    }

    fun mockMethod(
        className: String,
        name: String,
        retType: String, args: List<ParameterSpec>
    ): String {
        return "return ($retType)$CLASS_PACKAGE.$CLASS_NAME.INSTANCE.$METHOD_MOCK(" +
                "\"$className\",\"$name\",\"$retType\",${getArgs(args)})"
    }

    fun mockVoid(
        className: String,
        name: String,
        args: List<ParameterSpec>
    ): String {
        return "$CLASS_PACKAGE.$CLASS_NAME.INSTANCE.$METHOD_VOID_MOCK(\"$className\",\"$name\",${
            getArgs(
                args
            )
        })"
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