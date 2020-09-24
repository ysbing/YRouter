package com.ysbing.yrouter.plugin

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.variant.VariantInfo
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.pipeline.TransformManager
import com.ysbing.yrouter.core.util.DecompileUtil
import com.ysbing.yrouter.core.util.MakeJarUtil
import org.gradle.api.Project
import java.io.File
import java.util.*

class FindUsagesTransform(
    private val project: Project,
    private val library: LibraryExtension
) :
    Transform() {
    companion object {
        const val INDEX_USAGES_FILE = "index_usages.txt"
    }

    override fun getName(): String {
        return "FindUsagesTransform"
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

    override fun applyToVariant(variant: VariantInfo): Boolean {
        return !variant.isDebuggable
    }

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        val buildDir = transformInvocation.context.temporaryDir
        val indexFile = ArrayList<File>()
        val usagesInfo = HashSet<String>()
        project.configurations.getAt(YRouterPlugin.YROUTER).asPath.split(";").map {
            indexFile.add(File(it))
        }
        transformInvocation.inputs?.map {
            it.directoryInputs.map { dir ->
                val dest = transformInvocation.outputProvider.getContentLocation(
                    dir.name,
                    dir.contentTypes,
                    dir.scopes,
                    Format.DIRECTORY
                )
                dir.file.copyRecursively(dest, true)
                findUsages(indexFile, buildDir, dir.file, usagesInfo)
            }
            it.jarInputs.map { jar ->
                val dest = transformInvocation.outputProvider.getContentLocation(
                    jar.name,
                    jar.contentTypes,
                    jar.scopes,
                    Format.JAR
                )
                jar.file.copyTo(dest, true)
            }
        }
        val usagesInfoFile = File(
            project.buildDir,
            "${YRouterPlugin.YROUTER}${File.separator}$INDEX_USAGES_FILE"
        )
        usagesInfoFile.parentFile.mkdirs()
        usagesInfoFile.deleteRecursively()
        usagesInfo.map {
            usagesInfoFile.appendText(it + "\n")
        }
        val variantName = transformInvocation.context.variantName
        library.libraryVariants.map { variant ->
            if (variant.name == variantName) {
                variant.packageLibraryProvider.get().from(usagesInfoFile)
            }
        }
    }

    private fun findUsages(
        indexFile: List<File>,
        buildDir: File,
        file: File,
        usagesInfo: HashSet<String>
    ) {
        try {
            if (file.isDirectory) {
                val zipFile = File(buildDir, "${System.currentTimeMillis()}.jar")
                MakeJarUtil.buildJar(file, zipFile)
                DecompileUtil.run(indexFile, zipFile, usagesInfo)
                return
            }
            DecompileUtil.run(indexFile, file, usagesInfo)
        } catch (e: Throwable) {
        }
    }
}
