package com.ysbing.yrouter.plugin

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.gradle.internal.pipeline.TransformManager
import com.ysbing.yrouter.core.FindClass
import com.ysbing.yrouter.core.util.FileOperation
import com.ysbing.yrouter.core.util.MakeJarUtil
import com.ysbing.yrouter.core.util.Md5Util
import com.ysbing.yrouter.plugin.Constants.YROUTER
import org.gradle.api.Project
import java.io.File
import java.util.*

class FilterSelfClassTransform(private val project: Project) : Transform() {

    override fun getName(): String {
        return "FilterSelfClassTransform"
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

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        transformInvocation.outputProvider.deleteAll()
        transformInvocation.context.temporaryDir.deleteRecursively()
        val indexFile = ArrayList<String>()
        val preCopyFileMap = mutableMapOf<String, Pair<File, File>>()
        project.configurations.getAt(YROUTER).asPath.split(";").map {
            indexFile.add(Md5Util.getMD5Str(File(it)))
        }
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
                    "${System.nanoTime()}.jar"
                )
                MakeJarUtil.buildJar(dir.file, zipFile)
                findClass.add(zipFile)
            }
            it.jarInputs.map { jar ->
                val md5 = Md5Util.getMD5Str(jar.file)
                val dest = transformInvocation.outputProvider.getContentLocation(
                    jar.name,
                    jar.contentTypes,
                    jar.scopes,
                    Format.JAR
                )
                if (!indexFile.contains(md5)) {
                    jar.file.copyTo(dest, true)
                    findClass.add(jar.file)
                } else if (!preCopyFileMap.containsKey(md5)) {
                    preCopyFileMap[md5] = Pair(jar.file, dest)
                }
            }
        }
        findClass.load()
        preCopyFileMap.keys.map {
            val pair = preCopyFileMap[it] ?: return@map
            val unzipDir = File(transformInvocation.context.temporaryDir, it)
            FileOperation.unZipAPk(pair.first.absolutePath, unzipDir.absolutePath)
            filterClass(findClass, unzipDir, unzipDir.absolutePath)
            MakeJarUtil.buildJar(unzipDir, pair.second)
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
                    .replace(File.separator, ".")
                if (findClass.hasClass(className)) {
                    file.deleteRecursively()
                }
            }
        }
    }
}
