package com.ysbing.yrouter.test.kotlin

import androidx.fragment.app.FragmentActivity
import com.ysbing.yrouter.api.YRouterApi

/**
 * 继承一个通配符泛型系统类
 */
@YRouterApi
class Kotlin14 : ArrayList<Set<out FragmentActivity>>() {
    override fun add(element: Set<out FragmentActivity>): Boolean {
        return super.add(element)
    }
}