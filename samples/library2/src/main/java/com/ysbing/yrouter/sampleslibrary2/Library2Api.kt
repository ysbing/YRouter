package com.ysbing.yrouter.sampleslibrary2

import com.ysbing.yrouter.api.YRouterApi

class Library2Api {
    @YRouterApi
    var a: String = "123"

    @YRouterApi
    fun b(num: Float): String {
        return "abc"
    }
}