package com.ysbing.yrouter.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.ysbing.yrouter.plugin.Constants.YROUTER_API_DEPENDENCIES
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin

class YRouterSystemPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.dependencies.add(
            JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
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