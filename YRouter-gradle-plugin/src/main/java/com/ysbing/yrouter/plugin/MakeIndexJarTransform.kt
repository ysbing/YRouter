package com.ysbing.yrouter.plugin

import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import com.ysbing.yrouter.core.DexBean
import com.ysbing.yrouter.core.ExtractDexClassObject
import com.ysbing.yrouter.core.util.MakeJarUtil
import com.ysbing.yrouter.core.util.WriteCodeUtil
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.KotlinBasePluginWrapper
import java.io.File
import java.net.URLClassLoader


class MakeIndexJarTransform(
    private val project: Project,
    private val android: AppExtension
) : Transform() {
    companion object {
        const val KOTLIN_STDLIB = "kotlin-stdlib"
    }

    override fun getName(): String {
        return "MakeIndexJarTransform"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return TransformManager.SCOPE_FULL_PROJECT
    }

    override fun isIncremental(): Boolean {
        return false
    }

    private fun getKotlinStdlibClassPath(): File? {
        val pluginVersion = project.plugins.filterIsInstance<KotlinBasePluginWrapper>()
            .firstOrNull()?.kotlinPluginVersion
        val urlClassLoader = YRouterPlugin::class.java.classLoader as? URLClassLoader ?: return null
        return urlClassLoader.urLs
            .firstOrNull { it.toString().endsWith("$KOTLIN_STDLIB-$pluginVersion.jar") }
            ?.let { File(it.toURI()) }
            ?.takeIf(File::exists)
    }

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        transformInvocation.outputProvider.deleteAll()
        transformInvocation.context.temporaryDir.deleteRecursively()
        val buildDir = transformInvocation.context.temporaryDir
        val variantName = transformInvocation.context.variantName
        var apkOutputFile: File? = null
        android.applicationVariants.map { variant ->
            if (variant.name == variantName) {
                variant.outputs.map { output ->
                    apkOutputFile = output.outputFile
                }
            }
        }
        if (apkOutputFile == null) {
            return
        }
        val outputFileName = apkOutputFile?.name?.replace(".apk", "")?.replace("-debug", "")
        val outputFile = File(
            "${project.buildDir}${File.separator}${YRouterPlugin.YROUTER}",
            "/${outputFileName}-yrouter_index.jar"
        )
        val startTime = System.currentTimeMillis()
        val infoList = ArrayList<DexBean>()
        transformInvocation.inputs?.map {
            it.directoryInputs.map { dir ->
                collectDirClass(buildDir, dir.file, infoList)
            }
            it.jarInputs.map { jar ->
                collectDirClass(buildDir, jar.file, infoList)
            }
        }
        if (infoList.isEmpty()) {
            return
        }
        val writeCodeUtil = WriteCodeUtil(buildDir?.absolutePath + "/src")
        //生成java和kotlin源代码
        infoList.groupBy {
            it.classNode
        }.map {
            writeCodeUtil.run(it.key, it.value)
        }
        //编译源代码
        MakeJarUtil.buildJavaClass(
            buildDir?.absolutePath + "/src/lib",
            buildDir?.absolutePath + "/src/lib",
            arrayOf()
        )
        MakeJarUtil.buildKotlinClass(
            buildDir?.absolutePath + "/src/lib",
            buildDir?.absolutePath + "/src/lib",
            arrayOf(getKotlinStdlibClassPath()?.absolutePath)
        )
        MakeJarUtil.buildJavaClass(
            buildDir?.absolutePath + "/src/main",
            buildDir?.absolutePath + "/src/main",
            arrayOf(buildDir?.absolutePath + "/src/lib")
        )
        MakeJarUtil.buildKotlinClass(
            buildDir?.absolutePath + "/src/main",
            buildDir?.absolutePath + "/src/main",
            arrayOf(
                buildDir?.absolutePath + "/src/lib",
                getKotlinStdlibClassPath()?.absolutePath
            )
        )
        //将所有的class合jar
        MakeJarUtil.buildJar(File(buildDir?.absolutePath + "/src/main"), outputFile)
        println("yrouter build success->${outputFile.absolutePath}，耗时:${System.currentTimeMillis() - startTime}")
    }

    private fun collectDirClass(buildDir: File, file: File, infoList: MutableList<DexBean>) {
        try {
            if (file.isDirectory) {
                val zipFile = File(buildDir, "${System.currentTimeMillis()}.jar")
                MakeJarUtil.buildJar(file, zipFile)
                ExtractDexClassObject.run(zipFile, infoList)
                return
            }
            ExtractDexClassObject.run(file, infoList)
        } catch (e: Throwable) {
        }
    }
}
