package com.ysbing.yrouter.test.kotlin

import com.ysbing.yrouter.api.YRouterApi

/**
 * 内部类
 */
class Kotlin18 {
    private val a: Long = 0L

    private fun b() {}

    private inner class Kotlin18Inner1 {
        @YRouterApi
        private inner class Kotlin18Inner1_1 {
            private val a: String? = null
            fun b() {}
        }
    }

    @YRouterApi
    private inner class Kotlin18Inner2 {
        private val a = 0
        private val b: Float? = null
    }
}
