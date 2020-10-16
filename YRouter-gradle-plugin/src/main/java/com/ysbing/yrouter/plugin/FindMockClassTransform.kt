package com.ysbing.yrouter.plugin

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.QualifiedContent.DefaultContentType
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.google.common.collect.ImmutableSet
import com.google.common.collect.Sets
import com.google.gson.Gson
import com.ysbing.yrouter.core.FindClass
import com.ysbing.yrouter.core.util.MakeJarUtil
import com.ysbing.yrouter.core.util.WriteKotlinMockCodeUtil
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import java.io.File
import java.net.URLClassLoader

class FindMockClassTransform(private val project: Project) : Transform() {
    override fun getName(): String {
        return "FindMockClassTransform"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.PROJECT_ONLY
    }

    override fun isIncremental(): Boolean {
        return false
    }

    private fun getKotlinStdlibClassPath(): File? {
        val pluginVersion = project.plugins.filterIsInstance<KotlinBasePluginWrapper>()
            .firstOrNull()?.kotlinPluginVersion
        val urlClassLoader = YRouterPlugin::class.java.classLoader as? URLClassLoader ?: return null
        return urlClassLoader.urLs
            .firstOrNull {
                it.toString().endsWith("${MakeIndexJarTransform.KOTLIN_STDLIB}-$pluginVersion.jar")
            }
            ?.let { File(it.toURI()) }
            ?.takeIf(File::exists)
    }

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        transformInvocation.outputProvider.deleteAll()
        transformInvocation.context.temporaryDir.deleteRecursively()
        val findClass = FindClass()
        transformInvocation.inputs?.map {
            it.directoryInputs.map { dir ->
                val dest = transformInvocation.outputProvider.getContentLocation(
                    dir.name,
                    dir.contentTypes,
                    dir.scopes,
                    Format.DIRECTORY
                )
                dir.file.copyRecursively(dest, true)
                val zipFile = File(
                    transformInvocation.context.temporaryDir,
                    "${System.currentTimeMillis()}.jar"
                )
                MakeJarUtil.buildJar(dir.file, zipFile)
                findClass.add(zipFile)
            }
            it.jarInputs.map { jar ->
                val dest = transformInvocation.outputProvider.getContentLocation(
                    jar.name,
                    jar.contentTypes,
                    jar.scopes,
                    Format.JAR
                )
                jar.file.copyTo(dest, true)
                findClass.add(jar.file)
            }
        }
        findClass.load()
        val mockClassArray = findClass.getMockClassArray()
        val json = Gson().toJson(mockClassArray)
        val lib = File(transformInvocation.context.temporaryDir, "lib")
        WriteKotlinMockCodeUtil.writeMockConfigJava(lib, json)
        MakeJarUtil.buildKotlinClass(
            lib.absolutePath, lib.absolutePath,
            arrayOf(getKotlinStdlibClassPath()?.absolutePath)
        )
        val mockDest = transformInvocation.outputProvider.getContentLocation(
            System.currentTimeMillis().toString(),
            ImmutableSet.of<QualifiedContent.ContentType>(DefaultContentType.CLASSES),
            Sets.immutableEnumSet(QualifiedContent.Scope.EXTERNAL_LIBRARIES),
            Format.JAR
        )
        MakeJarUtil.buildJar(lib, mockDest)
    }
}