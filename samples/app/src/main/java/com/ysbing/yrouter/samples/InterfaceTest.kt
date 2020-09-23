package com.ysbing.yrouter.samples

import android.content.Context
import android.widget.Toast
import com.ysbing.yrouter.api.YRouterApi

interface InterfaceTest {
    @YRouterApi
    fun meme()

    @YRouterApi
    fun kkk(context: Context) {
        Toast.makeText(context, "InterfaceTest.kkk", Toast.LENGTH_SHORT).show()
    }
}