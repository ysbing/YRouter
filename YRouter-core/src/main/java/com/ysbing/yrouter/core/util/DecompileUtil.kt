package com.ysbing.yrouter.core.util

import jadx.api.JadxArgs
import jadx.core.dex.nodes.ClassNode
import jadx.core.dex.nodes.FieldNode
import jadx.core.dex.nodes.MethodNode
import jadx.core.dex.nodes.RootNode
import jadx.core.utils.files.InputFile
import java.io.File

/**
 * 将解析后的dex，写入Java文件
 */
object DecompileUtil {

    fun run(indexJar: List<File>, localClass: File, usagesInfo: HashSet<String>) {
        val inputFiles = arrayListOf<File>()
        inputFiles.add(localClass)
        inputFiles.addAll(indexJar)
        val sourceUsagesInfo = search(inputFiles, indexJar = indexJar, localClassFile = localClass)
        val indexUsagesInfo = search(indexJar)
        sourceUsagesInfo.intersect(indexUsagesInfo).apply {
            usagesInfo.addAll(this)
        }
    }

    fun search(
        files: List<File>,
        indexJar: List<File>? = null,
        localClassFile: File? = null
    ): HashSet<String> {
        val usagesInfo = HashSet<String>()
        val args = JadxArgs()
        args.inputFiles = files
        val loadedInputs = ArrayList<InputFile>()
        files.map {
            InputFile.addFilesFrom(it, loadedInputs, true)
        }
        val root = RootNode(args)
        root.load(loadedInputs)
        root.initClassPath()
        root.initPasses()
        root.getClasses(false).map { classNode ->
            decompile(classNode, usagesInfo, indexJar, localClassFile)
        }
        return usagesInfo
    }

    private fun decompile(
        classNode: ClassNode,
        usagesInfo: HashSet<String>,
        indexJar: List<File>?,
        localClassFile: File? = null,
    ) {
        val codeInfo = classNode.decompile()
        codeInfo.annotations.map {
            val node = it.value
            var isAdd = true
            if (indexJar?.contains(classNode.dex().dexFile.inputFile.file) == true) {
                isAdd = false
            } else if (node is ClassNode) {
                if (localClassFile == node.dex().dexFile.inputFile.file) {
                    isAdd = false
                }
            } else if (node is MethodNode) {
                if (localClassFile == node.dex().dexFile.inputFile.file) {
                    isAdd = false
                }
            } else if (node is FieldNode) {
                if (localClassFile == node.dex().dexFile.inputFile.file) {
                    isAdd = false
                }
            }
            if (isAdd) {
                usagesInfo.add(it.value.toString())
            }
        }
    }
}