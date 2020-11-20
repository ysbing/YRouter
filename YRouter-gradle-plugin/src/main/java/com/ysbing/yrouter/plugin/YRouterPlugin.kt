package com.ysbing.yrouter.plugin

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import com.ysbing.yrouter.plugin.Constants.YROUTER
import com.ysbing.yrouter.plugin.Constants.YROUTER_API_DEPENDENCIES
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPlugin


open class YRouterPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        createConfiguration(project)
        if (project.plugins.hasPlugin(AppPlugin::class.java)) {
            val android = project.extensions.getByType(AppExtension::class.java)
            android.sourceSets.all { sourceSet ->
                createConfiguration(project, sourceSet.name)
            }
            project.afterEvaluate {
                createApkTask(project)
                android.applicationVariants.map { variant ->
                    if (variant.buildType.isDebuggable) {
                        createApkTask(project, variant.name)
                    }
                }
            }
            var hasYRouterTask = false
            project.gradle.startParameter.taskNames.map {
                if (it.contains(YROUTER)) {
                    hasYRouterTask = true
                }
            }
            android.registerTransform(
                CheckUsagesTransform(
                    project,
                    android,
                    this is YRouterMockPlugin
                )
            )
            if (hasYRouterTask) {
                android.registerTransform(MakeIndexJarTransform(project, android))
            }
        } else if (project.plugins.hasPlugin(LibraryPlugin::class.java)) {
            val library = project.extensions.getByType(LibraryExtension::class.java)
            library.registerTransform(FindUsagesTransform(project, library))
            library.sourceSets.all { sourceSet ->
                createConfiguration(project, sourceSet.name)
            }
        }
    }

    private fun createConfiguration(project: Project, variantName: String = "") {
        val configurationName =
            if (variantName.isEmpty()) YROUTER else variantName.decapitalize() + YROUTER.capitalize()
        val compileOnlyConfig =
            if (this is YRouterMockPlugin && project.plugins.hasPlugin(AppPlugin::class.java)) {
                if (variantName.isEmpty()) JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME
                else variantName.decapitalize() + JavaPlugin.IMPLEMENTATION_CONFIGURATION_NAME.capitalize()
            } else {
                if (variantName.isEmpty()) JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME
                else variantName.decapitalize() + JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME.capitalize()
            }
        if (project.configurations.findByName(configurationName) == null
            && project.configurations.findByName(compileOnlyConfig) != null
        ) {
            val configuration = project.configurations.create(configurationName)
            project.configurations.getByName(compileOnlyConfig).extendsFrom(configuration)
        }
        if (variantName.isEmpty()) {
            project.dependencies.add(
                JavaPlugin.COMPILE_ONLY_CONFIGURATION_NAME,
                YROUTER_API_DEPENDENCIES
            )
        }
    }

    private fun createApkTask(project: Project, variant: String = "") {
        val variantName = variant.capitalize().replace("Debug", "").replace("Release", "")
        val taskName = "${YROUTER}${variantName}"
        if (project.tasks.findByPath(taskName) == null) {
            project.task(taskName).apply {
                group = YROUTER
                dependsOn("assemble${variantName}Debug")
            }
        }
    }
}