package com.ysbing.yrouter.test.kotlin

import android.app.Activity
import android.content.Context
import com.ysbing.yrouter.api.YRouterApi

/**
 * 开放方法或变量
 */
class Kotlin06 {
    val a = 0

    @YRouterApi
    val b = 0f

    fun c() {}

    @YRouterApi
    fun d(num: Double, context: Context?): Activity? {
        return null
    }
}