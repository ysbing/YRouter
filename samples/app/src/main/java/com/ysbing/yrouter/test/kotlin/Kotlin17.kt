package com.ysbing.yrouter.test.kotlin

import android.app.Activity
import android.app.Fragment
import com.ysbing.yrouter.api.YRouterApi

/**
 * 继承系统类，系统类实现了接口
 */
@YRouterApi
class Kotlin17 : Fragment(), Kotlin17InterfaceTest

@YRouterApi
private interface Kotlin17InterfaceTest {
    fun getActivity(): Activity?
}