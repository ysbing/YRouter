package com.ysbing.yrouter.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin


class YRouterMockPlugin : YRouterPlugin() {
    override fun apply(project: Project) {
        if (project.plugins.hasPlugin(AppPlugin::class.java)) {
            project.dependencies.add(
                JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME,
                YROUTER_MOCK_DEPENDENCIES
            )
            val android = project.extensions.getByType(AppExtension::class.java)
            android.registerTransform(FindMockClassTransform(project))
            android.registerTransform(FilterSelfClassTransform(project))
        }
        super.apply(project)
    }
}