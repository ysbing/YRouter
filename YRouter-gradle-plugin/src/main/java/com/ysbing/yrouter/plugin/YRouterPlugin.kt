package com.ysbing.yrouter.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project


class YRouterPlugin : Plugin<Project> {
    companion object {
        const val YROUTER = "yrouter"
        const val YROUTER_API_DEPENDENCIES = "com.ysbing.yrouter:YRouter-api:1.0.1"
    }

    override fun apply(project: Project) {
        createConfiguration(project)
        if (project.plugins.hasPlugin(AppPlugin::class.java)) {
            val android = project.extensions.getByType(AppExtension::class.java)
            project.afterEvaluate {
                createApkTask(project, "")
                android.applicationVariants.map { variant ->
                    if (variant.buildType.isDebuggable) {
                        val variantName = variant.name.capitalize().replace("Debug", "")
                        createApkTask(project, variantName)
                    }
                }
            }
            var hasYRouterTask = false
            project.gradle.startParameter.taskNames.map {
                if (it.contains(YROUTER)) {
                    hasYRouterTask = true
                }
            }
            android.registerTransform(CheckUsagesTransform(project, android))
            if (hasYRouterTask) {
                android.registerTransform(MakeIndexJarTransform(project, android))
            }
        } else if (project.plugins.hasPlugin(LibraryPlugin::class.java)) {
            val library = project.extensions.getByType(LibraryExtension::class.java)
            library.registerTransform(FindUsagesTransform(project, library))
        }
    }

    private fun createConfiguration(project: Project) {
        val configuration = project.configurations.create(YROUTER)
        project.configurations.getByName("compileOnly").extendsFrom(configuration)
        project.dependencies.add("api", YROUTER_API_DEPENDENCIES)
    }

    private fun createApkTask(project: Project, variantName: String) {
        val taskName = "${YROUTER}${variantName}"
        if (project.tasks.findByPath(taskName) == null) {
            project.task(taskName).apply {
                group = YROUTER
                dependsOn("assemble${variantName}Debug")
            }
        }
    }
}