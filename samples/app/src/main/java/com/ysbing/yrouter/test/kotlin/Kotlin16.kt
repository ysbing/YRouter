package com.ysbing.yrouter.test.kotlin

import com.ysbing.yrouter.api.YRouterApi


/**
 * 父类实现接口类，直接继承，父类不开发API
 */
@YRouterApi
class Kotlin16 : Kotlin16Parent(), Kotlin16InterfaceTest{
}

open class Kotlin16Parent {
    fun a() {}
}

@YRouterApi
private interface Kotlin16InterfaceTest {
    fun a()
}