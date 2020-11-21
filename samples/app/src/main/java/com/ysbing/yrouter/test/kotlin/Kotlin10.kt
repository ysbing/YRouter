package com.ysbing.yrouter.test.kotlin

import androidx.fragment.app.Fragment
import com.ysbing.yrouter.api.YRouterApi

/**
 * 实现一个指定类型的泛型系统接口
 */
@YRouterApi
class Kotlin10 : Comparator<Fragment> {
    override fun compare(o1: Fragment?, o2: Fragment?): Int = 0
}