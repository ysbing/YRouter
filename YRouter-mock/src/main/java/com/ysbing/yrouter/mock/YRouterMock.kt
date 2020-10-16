package com.ysbing.yrouter.mock

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.ysbing.yrouter.api.mock.YRouterMockBean
import com.ysbing.yrouter.api.mock.YRouterMockPair

object YRouterMock {

    var mockClass: List<YRouterMockBean>? = null

    fun fieldMock(
        className: String,
        name: String,
        retType: String
    ): Any? {
        val ret = getFieldRet(className, name)
        if (ret != null) {
            return ret
        }
        return getDefaultRet(retType)
    }

    fun methodMock(
        className: String,
        name: String,
        retType: String,
        vararg args: YRouterMockPair
    ): Any? {
        val ret = getMethodRet(className, name, *args)
        if (ret != null) {
            return ret
        }
        return getDefaultRet(retType)
    }

    fun methodVoidMock(className: String, name: String, vararg args: YRouterMockPair) {
        getMethodRet(className, name, *args)
    }

    private fun getFieldRet(
        className: String,
        name: String
    ): Any? {
        val mockClass = getMockClassArray()
        if (mockClass?.isNotEmpty() == true) {
            mockClass.groupBy {
                className
            }.map {
                it.value.map { mock ->
                    if (mock.type == YRouterMockBean.TYPE.FIELD && mock.targetName == name) {
                        val clazz = getClass(mock.className)
                        val field = clazz.getDeclaredField(mock.name)
                        field.isAccessible = true
                        return field.get(clazz.newInstance())
                    }
                }
            }
        }
        return null
    }

    private fun getMethodRet(
        className: String,
        name: String,
        vararg args: YRouterMockPair
    ): Any? {
        val mockClass = getMockClassArray()
        if (mockClass?.isNotEmpty() == true) {
            mockClass.groupBy {
                className
            }.map {
                it.value.map { mock ->
                    if (mock.type == YRouterMockBean.TYPE.METHOD && mock.targetName == name) {
                        val clazz = Class.forName(mock.className)
                        val argsType = mutableListOf<Class<*>>()
                        val argsParams = mutableListOf<Any?>()
                        args.map { pair ->
                            argsType.add(getClass(pair.className))
                            argsParams.add(pair.args)
                        }
                        val method = clazz.getDeclaredMethod(mock.name, *argsType.toTypedArray())
                        method.isAccessible = true
                        return method.invoke(clazz.newInstance(), *argsParams.toTypedArray())
                    }
                }
            }
        }
        return null
    }

    private fun getDefaultRet(className: String): Any? {
        return when (className) {
            Boolean::class.java.name -> {
                false
            }
            Char::class.java.name -> {
                '0'
            }
            Byte::class.java.name -> {
                0
            }
            Short::class.java.name -> {
                0
            }
            Int::class.java.name -> {
                0
            }
            Float::class.java.name -> {
                0f
            }
            Long::class.java.name -> {
                0L
            }
            Double::class.java.name -> {
                0f
            }
            "string", String::class.java.name -> {
                "\"\""
            }
            else -> {
                null
            }
        }
    }

    private fun getMockClassArray(): List<YRouterMockBean>? {
        if (mockClass != null) {
            return mockClass
        }
        try {
            val mockConfigClass = Class.forName("com.ysbing.yrouter.mock.YRouterMockConfig")
            val configField = mockConfigClass.getDeclaredField("config")
            val config = configField.get(null) as String
            mockClass = Gson().fromJson(config, object : TypeToken<List<YRouterMockBean>>() {}.type)
            return mockClass
        } catch (e: Exception) {
        }
        return null
    }

    private fun getClass(_className: String): Class<*> {
        return when (_className.replace("kotlin.", "").decapitalize()) {
            Boolean::class.java.name -> {
                Boolean::class.java
            }
            Char::class.java.name -> {
                Char::class.java
            }
            Byte::class.java.name -> {
                Byte::class.java
            }
            Short::class.java.name -> {
                Short::class.java
            }
            Int::class.java.name -> {
                Int::class.java
            }
            Float::class.java.name -> {
                Float::class.java
            }
            Long::class.java.name -> {
                Long::class.java
            }
            Double::class.java.name -> {
                Double::class.java
            }
            "string", String::class.java.name -> {
                String::class.java
            }
            else -> {
                Class.forName(_className)
            }
        }
    }
}