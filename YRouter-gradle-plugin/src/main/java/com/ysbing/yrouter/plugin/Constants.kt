package com.ysbing.yrouter.plugin

import com.android.Version

object Constants {
    const val YROUTER = "yrouter"
    const val YROUTER_VERSION = "1.2.0"
    const val YROUTER_API_DEPENDENCIES =
        "com.ysbing.yrouter:YRouter-api:$YROUTER_VERSION"
    const val YROUTER_MOCK_DEPENDENCIES =
        "com.ysbing.yrouter:YRouter-mock:$YROUTER_VERSION"
    val ANDROID_GRADLE_PLUGIN_VERSION: String = try {
        Version.ANDROID_GRADLE_PLUGIN_VERSION
    } catch (e: Throwable) {
        com.android.builder.model.Version.ANDROID_GRADLE_PLUGIN_VERSION
    }
}