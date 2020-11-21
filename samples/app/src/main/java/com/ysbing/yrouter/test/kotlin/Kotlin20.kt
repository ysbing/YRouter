package com.ysbing.yrouter.test.kotlin

import com.ysbing.yrouter.api.YRouterApi

/**
 * 扩展方法，复杂泛型
 */
@YRouterApi
interface Kotlin20Parent<T : Kotlin20Generic<T>> {
    fun a(a: T)
}

@YRouterApi
interface Kotlin20Generic<in T> {
    fun a(a: T): Int
}

@YRouterApi
fun <T : Kotlin20Generic<T>> a(): Kotlin20Parent<T> = object : Kotlin20Parent<T> {
    override fun a(a: T) {
    }
}