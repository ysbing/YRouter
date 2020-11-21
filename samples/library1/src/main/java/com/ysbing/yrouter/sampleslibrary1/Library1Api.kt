package com.ysbing.yrouter.sampleslibrary1

import com.ysbing.yrouter.api.YRouterApi
import com.ysbing.yrouter.samples.MainActivity

class Library1Api {
    @YRouterApi
    var a: String = "a123"

    @YRouterApi
    fun b(a: Map<String, Double>, b: Float): Int {
        return 0
    }

    @YRouterApi
    fun c(num: Int): String {
        return MainActivity.getName() + num
    }
}