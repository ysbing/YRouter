package com.ysbing.yrouter.test

import android.widget.Toast
import com.ysbing.yrouter.api.mock.YRouterMockClass
import com.ysbing.yrouter.api.mock.YRouterMockValue
import com.ysbing.yrouter.mock.ContextHelper
import com.ysbing.yrouter.sampleslibrary2.Library2Api

/**
 * 模拟Library2Api类的方法，便于在单个模块内测试
 */
@YRouterMockClass(Library2Api::class)
class MockTest {
    @YRouterMockValue("a")
    private val a = "abc"

    @YRouterMockValue("b")
    fun b(num: Float): String {
        Toast.makeText(ContextHelper.getAppContext(), "hello:$num", Toast.LENGTH_SHORT).show()
        return ""
    }
}