package com.ysbing.yrouter.test.kotlin

import android.app.Activity
import android.content.Context
import androidx.fragment.app.Fragment
import com.ysbing.yrouter.api.YRouterApi
import com.ysbing.yrouter.api.YRouterSkip

/**
 * 开放类内所有方法和变量
 */
@YRouterApi
class Kotlin07 {
    private constructor()
    constructor(fragment: Fragment)

    val a = 0

    @YRouterSkip
    val b = 0f

    @YRouterSkip
    fun c() {
    }

    fun d(num: Double, context: Context?): Activity? {
        return null
    }
}