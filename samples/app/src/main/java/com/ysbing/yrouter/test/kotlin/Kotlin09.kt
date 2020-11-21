package com.ysbing.yrouter.test.kotlin

import com.ysbing.yrouter.api.YRouterApi

/**
 * 实现一个未指定类型的泛型系统接口
 */
@YRouterApi
class Kotlin09<T> : Comparable<T> {
    override fun compareTo(other: T): Int = 0
}