package com.ysbing.yrouter.test.kotlin

import com.ysbing.yrouter.api.YRouterApi

/**
 * 抽象类实现接口
 */
@YRouterApi
abstract class Kotlin15 : Kotlin15InterfaceTest {
    override fun a() {}
}

interface Kotlin15InterfaceTest {
    fun a()
}