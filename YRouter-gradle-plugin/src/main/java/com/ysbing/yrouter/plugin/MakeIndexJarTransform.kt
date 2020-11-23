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
import com.ysbing.yrouter.plugin.Constants.YROUTER
import org.gradle.api.Project
import java.io.File
import java.net.URLClassLoader


class MakeIndexJarTransform(
    private val project: Project,
    private val android: AppExtension
) : Transform() {
    companion object {
        const val KOTLIN_STDLIB = "kotlin-stdlib"
    }

    private val mKotlinStdlibClassPath: String? by lazy {
        getKotlinStdlibClassPath()?.absolutePath
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
        return if (project.plugins.hasPlugin("kotlin-android")) {
            val urlClassLoader =
                YRouterPlugin::class.java.classLoader as? URLClassLoader ?: return null
            urlClassLoader.urLs
                .firstOrNull {
                    it.toString().contains("$KOTLIN_STDLIB-\\d+(\\.\\d+)*.jar".toRegex())
                }
                ?.let { File(it.toURI()) }
                ?.takeIf(File::exists)
        } else {
            null
        }
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

        val startTime = System.currentTimeMillis()
        val infoList = ArrayList<DexBean>()
        val extractDexClassObject = ExtractDexClassObject(infoList)
        transformInvocation.inputs?.map {
            it.directoryInputs.map { dir ->
                collectDirClass(buildDir, dir.file, extractDexClassObject)
            }
            it.jarInputs.map { jar ->
                collectDirClass(buildDir, jar.file, extractDexClassObject)
            }
        }
        extractDexClassObject.run()
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
        val outputFile = File(
            "${project.buildDir}${File.separator}${YROUTER}",
            "/${outputFileName}-yrouter_index.jar"
        )
        val libDir = File(buildDir?.absolutePath + "/src/lib")
        val mainDir = File(buildDir?.absolutePath + "/src/main")
        val classDir = File(buildDir?.absolutePath + "/class")
        libDir.mkdirs()
        mainDir.mkdirs()
        classDir.mkdirs()
        //编译源代码
        MakeJarUtil.buildJavaClass(libDir, libDir, arrayOf())
        MakeJarUtil.buildKotlinClass(libDir, libDir, arrayOf(mKotlinStdlibClassPath))
        MakeJarUtil.buildJavaClass(mainDir, classDir, arrayOf(libDir))
        MakeJarUtil.buildKotlinClass(
            mainDir,
            classDir,
            arrayOf(libDir.absolutePath, mKotlinStdlibClassPath)
        )
        //将所有的class合jar
        MakeJarUtil.buildJar(classDir, outputFile)
        println("yrouter build success->${outputFile.absolutePath}，耗时:${System.currentTimeMillis() - startTime}")
    }

    private fun collectDirClass(
        buildDir: File,
        file: File,
        extractDexClassObject: ExtractDexClassObject
    ) {
        try {
            if (file.isDirectory) {
                val zipFile = File(buildDir, "${System.nanoTime()}.jar")
                MakeJarUtil.buildJar(file, zipFile)
                extractDexClassObject.load(zipFile)
                return
            }
            extractDexClassObject.load(file)
        } catch (e: Throwable) {
        }
    }
}
