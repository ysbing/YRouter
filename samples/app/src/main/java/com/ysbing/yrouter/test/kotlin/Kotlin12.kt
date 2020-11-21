package com.ysbing.yrouter.test.kotlin

import com.ysbing.yrouter.api.YRouterApi
import java.io.File

/**
 * 继承一个系统类必须调super
 */
@YRouterApi
class Kotlin12(pathname: String) : File(pathname)