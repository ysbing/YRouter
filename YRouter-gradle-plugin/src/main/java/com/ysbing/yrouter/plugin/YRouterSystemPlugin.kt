package com.ysbing.yrouter.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class YRouterSystemPlugin : YRouterPlugin() {
    override fun apply(project: Project) {
        super.apply(project)
        project.dependencies.add(
            JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
            YROUTER_API_DEPENDENCIES
        )
        if (project.plugins.hasPlugin(AppPlugin::class.java)) {
            val android = project.extensions.getByType(AppExtension::class.java)
            android.registerTransform(FilterYRouterSystemClassTransform(project))
        } else if (project.plugins.hasPlugin(LibraryPlugin::class.java)) {
            val library = project.extensions.getByType(LibraryExtension::class.java)
            library.registerTransform(FilterYRouterSystemClassTransform(project))
        }
    }
}