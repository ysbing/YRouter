package com.ysbing.yrouter.test.kotlin

import com.ysbing.yrouter.api.YRouterApi
import java.lang.ref.SoftReference

/**
 * 泛型构造函数，但不支持具体类型，参考Java19
 */
@YRouterApi
class Kotlin21<T>(referent: T) : SoftReference<T>(referent)