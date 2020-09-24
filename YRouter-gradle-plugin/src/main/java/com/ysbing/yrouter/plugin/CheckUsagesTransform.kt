package com.ysbing.yrouter.plugin

import com.android.build.api.transform.Format
import com.android.build.api.transform.QualifiedContent
import com.android.build.api.transform.Transform
import com.android.build.api.transform.TransformInvocation
import com.android.build.api.variant.VariantInfo
import com.android.build.gradle.AppExtension
import com.android.build.gradle.internal.api.ApplicationVariantImpl
import com.android.build.gradle.internal.pipeline.TransformManager
import com.android.build.gradle.internal.publishing.AndroidArtifacts
import com.ysbing.yrouter.core.CheckClassObject
import com.ysbing.yrouter.core.util.FileOperation
import org.gradle.api.Project
import org.gradle.api.internal.artifacts.dependencies.DefaultProjectDependency
import java.io.File
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.HashSet


class CheckUsagesTransform(
    private val project: Project,
    private val android: AppExtension
) : Transform() {

    override fun getName(): String {
        return "CheckUsagesTransform"
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

    override fun applyToVariant(variant: VariantInfo): Boolean {
        return !variant.isDebuggable
    }

    override fun transform(transformInvocation: TransformInvocation) {
        super.transform(transformInvocation)
        val variantName = transformInvocation.context.variantName
        val usagesInfo = HashSet<String>()
        android.applicationVariants.map { variant ->
            if (variant.name == variantName) {
                if (variant is ApplicationVariantImpl) {
                    val aars = variant.variantData.scope.getArtifactCollection(
                        AndroidArtifacts.ConsumedConfigType.COMPILE_CLASSPATH,
                        AndroidArtifacts.ArtifactScope.ALL,
                        AndroidArtifacts.ArtifactType.EXPLODED_AAR
                    )
                    aars.artifacts.map { aar ->
                        val file = File(aar.file, FindUsagesTransform.INDEX_USAGES_FILE)
                        if (file.exists() && file.canRead()) {
                            println("找到引用配置:$file")
                            usagesInfo.addAll(file.readLines())
                        }
                    }
                }
            }
        }
        fun findProject(name: String) {
            project.configurations.getAt(name).dependencies.map { depend ->
                if (depend is DefaultProjectDependency) {
                    val file = File(
                        depend.dependencyProject.buildDir,
                        "${YRouterPlugin.YROUTER}${File.separator}${FindUsagesTransform.INDEX_USAGES_FILE}"
                    )
                    if (file.exists() && file.canRead()) {
                        println("找到引用配置:$file")
                        usagesInfo.addAll(file.readLines())
                    }
                }
            }
        }
        findProject("api")
        findProject("implementation")
        val classInfo = checkUsages(usagesInfo)
        val extractFiles = ArrayList<File>()
        val extractClass = ArrayList<String>()
        transformInvocation.inputs?.map {
            it.directoryInputs.map { dir ->
                val dest = transformInvocation.outputProvider.getContentLocation(
                    dir.name,
                    dir.contentTypes,
                    dir.scopes,
                    Format.DIRECTORY
                )
                dir.file.copyRecursively(dest, true)
                extractClass(
                    extractFiles,
                    extractClass,
                    classInfo,
                    dir.file,
                    transformInvocation.context.temporaryDir
                )
            }
            it.jarInputs.map { jar ->
                val dest = transformInvocation.outputProvider.getContentLocation(
                    jar.name,
                    jar.contentTypes,
                    jar.scopes,
                    Format.JAR
                )
                jar.file.copyTo(dest, true)
                extractClass(
                    extractFiles,
                    extractClass,
                    classInfo,
                    jar.file,
                    transformInvocation.context.temporaryDir
                )
            }
        }
        CheckClassObject.run(classInfo, extractFiles, extractClass)
    }

    private fun checkUsages(usagesInfo: HashSet<String>): MutableMap<String, MutableList<String>> {
        val map = TreeMap<String, MutableList<String>>()
        usagesInfo.map add@{ usage ->
            if (usage.contains("$")) {
                map[usage.substringBefore(" :")] = ArrayList()
            } else if (!usage.contains(":")) {
                map[usage] = ArrayList()
            } else {
                map.keys.reversed().map { key ->
                    if (usage.contains(key)) {
                        map[key]?.add(usage)
                        return@add
                    }
                }
            }
        }
        val changeMap = HashMap<String, String>()
        map.keys.map { className ->
            var hasKey: String? = null
            map.keys.map { key ->
                if (className != key && className.contains(key)) {
                    hasKey = key
                }
            }
            if (hasKey != null) {
                val newKey = "$hasKey\$${className.substringAfter("$hasKey.")}"
                changeMap[newKey] = className
            }
        }
        changeMap.map { change ->
            map.remove(change.value)?.let {
                map[change.key] = it
            }
        }
        return map
    }

    private fun extractClass(
        extractFiles: MutableList<File>,
        extractClass: MutableList<String>,
        classInfo: Map<String, MutableList<String>>,
        file: File,
        tmpDir: File
    ) {
        val dir: File
        if (file.isFile) {
            dir = tmpDir
            FileOperation.unZipAPk(file.absolutePath, dir.absolutePath)
        } else {
            dir = file
        }
        classInfo.keys.map {
            val className = it.replace(".", File.separator) + ".class"
            val classFile = File(dir, className)
            if (classFile.exists()) {
                extractClass.add(it)
                extractFiles.add(classFile)
            }
        }
    }
}
