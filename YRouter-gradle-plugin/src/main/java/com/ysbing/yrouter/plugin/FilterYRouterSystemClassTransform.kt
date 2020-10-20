package com.ysbing.yrouter.plugin

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.internal.pipeline.TransformManager
import com.ysbing.yrouter.core.FindClass
import com.ysbing.yrouter.core.util.FileOperation
import com.ysbing.yrouter.core.util.MakeJarUtil
import com.ysbing.yrouter.core.util.Md5Util
import org.gradle.api.Project
import java.io.File

class FilterYRouterSystemClassTransform(private val project: Project) : Transform() {

    override fun getName(): String {
        return "FilterYRouterSystemClassTransform"
    }

    override fun getInputTypes(): MutableSet<QualifiedContent.ContentType> {
        return TransformManager.CONTENT_CLASS
    }

    override fun getScopes(): MutableSet<in QualifiedContent.Scope> {
        return if (project.plugins.hasPlugin(AppPlugin::class.java))
            TransformManager.SCOPE_FULL_PROJECT else TransformManager.PROJECT_ONLY
    }

    override fun isIncremental(): Boolean {
        return false
    }

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        transformInvocation.outputProvider.deleteAll()
        transformInvocation.context.temporaryDir.deleteRecursively()
        val preCopyFileMap = mutableMapOf<String, Pair<File, File?>>()
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
                val md5 = Md5Util.getMD5Str(zipFile)
                if (!preCopyFileMap.containsKey(md5)) {
                    preCopyFileMap[md5] = Pair(dest, null)
                }
            }
            it.jarInputs.map { jar ->
                val md5 = Md5Util.getMD5Str(jar.file)
                val dest = transformInvocation.outputProvider.getContentLocation(
                    jar.name,
                    jar.contentTypes,
                    jar.scopes,
                    Format.JAR
                )
                findClass.add(jar.file)
                preCopyFileMap[md5] = Pair(jar.file, dest)
            }
        }
        findClass.load()
        preCopyFileMap.keys.map {
            val pair = preCopyFileMap[it] ?: return@map
            val unzipDir = File(transformInvocation.context.temporaryDir, it)
            if (pair.first.isFile) {
                FileOperation.unZipAPk(pair.first.absolutePath, unzipDir.absolutePath)
                filterClass(findClass, unzipDir, unzipDir.absolutePath)
            } else {
                filterClass(findClass, pair.first, pair.first.absolutePath)
            }
            pair.second?.let {
                MakeJarUtil.buildJar(unzipDir, pair.second)
            }
        }
    }

    private fun filterClass(findClass: FindClass, file: File, rootPath: String) {
        if (file.isDirectory) {
            file.listFiles()?.map {
                filterClass(findClass, it, rootPath)
            }
        } else {
            if (file.exists() && file.absolutePath.endsWith(".class")) {
                val className = file.absolutePath
                    .substringAfter("$rootPath${File.separator}")
                    .substringBeforeLast(".")
                    .replace("/", ".")
                if (findClass.hasYRouterSystem(className)) {
                    file.deleteRecursively()
                }
            }
        }
    }
}
