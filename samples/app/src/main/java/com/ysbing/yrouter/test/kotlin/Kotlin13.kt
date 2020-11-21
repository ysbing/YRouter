package com.ysbing.yrouter.test.kotlin

import androidx.fragment.app.FragmentActivity
import com.ysbing.yrouter.api.YRouterApi

/**
 * 继承一个带泛型的系统类
 */
@YRouterApi
class Kotlin13 : ArrayList<FragmentActivity>() {
    override fun add(element: FragmentActivity): Boolean {
        return super.add(element)
    }
}