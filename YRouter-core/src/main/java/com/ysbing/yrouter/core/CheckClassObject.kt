package com.ysbing.yrouter.core

import com.ysbing.yrouter.core.util.DecompileUtil
import java.io.File
import java.lang.RuntimeException


object CheckClassObject {

    fun run(
        classInfo: MutableMap<String, MutableList<String>>,
        extractFile: List<File>,
        extractClass: List<String>
    ) {
        classInfo.keys.subtract(extractClass).map {
            throw RuntimeException("${it}不存在，请检查工程")
        }
        val inputFiles = arrayListOf<File>()
        inputFiles.addAll(extractFile)
        val sourceUsagesInfo = DecompileUtil.search(inputFiles)
        classInfo.values.map { node ->
            node.map {
                if (!sourceUsagesInfo.contains(it)) {
                    throw RuntimeException("${it}不存在，请检查工程")
                }
            }
        }
    }
}