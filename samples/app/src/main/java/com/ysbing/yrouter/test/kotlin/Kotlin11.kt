package com.ysbing.yrouter.test.kotlin

import android.app.Activity
import androidx.fragment.app.Fragment
import com.ysbing.yrouter.api.YRouterApi

/**
 * 实现一个通配符泛型系统接口
 */
@YRouterApi
class Kotlin11 : Comparator<List<Map<out Activity, Fragment>>> {
    override fun compare(
        o1: List<Map<out Activity, Fragment>>?,
        o2: List<Map<out Activity, Fragment>>?
    ): Int = 0
}