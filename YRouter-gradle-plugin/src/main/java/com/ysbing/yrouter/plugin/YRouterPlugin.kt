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
        const val YROUTER_API_DEPENDENCIES = "com.ysbing.yrouter:YRouter-api:1.0.2-SNAPSHOT"
    }

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
            android.registerTransform(CheckUsagesTransform(project, android))
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
            if (variantName.isEmpty()) "compileOnly" else variantName.decapitalize() + "CompileOnly"
        if (project.configurations.findByName(configurationName) == null
            && project.configurations.findByName(compileOnlyConfig) != null
        ) {
            val configuration = project.configurations.create(configurationName)
            project.configurations.getByName(compileOnlyConfig).extendsFrom(configuration)
        }
        if (variantName.isEmpty()) {
            project.dependencies.add("api", YROUTER_API_DEPENDENCIES)
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