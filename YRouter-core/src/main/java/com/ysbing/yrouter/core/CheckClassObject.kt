package com.ysbing.yrouter.core

import com.ysbing.yrouter.core.util.DecompileUtil
import java.io.File
import java.util.*


object CheckClassObject {

    data class ClassInfoBean(
        var moduleName: String,
        var className: String
    ) : Comparable<ClassInfoBean> {
        override fun compareTo(other: ClassInfoBean): Int {
            return if (other.moduleName == this.moduleName && other.className == this.className) {
                0
            } else {
                -1
            }
        }
    }

    fun run(
        classInfo: TreeMap<ClassInfoBean, MutableList<String>>,
        extractFile: List<File>,
        extractClass: List<ClassInfoBean>
    ) {
        classInfo.keys.subtract(extractClass).map {
            throw RuntimeException("${it.moduleName}引用的${it.className}不存在，请检查工程")
        }
        val inputFiles = arrayListOf<File>()
        inputFiles.addAll(extractFile)
        val sourceUsagesInfo = DecompileUtil.search(inputFiles)
        classInfo.keys.map{ pair ->
            val node = classInfo[pair]
            node?.map {
                if (!sourceUsagesInfo.contains(it)) {
                    throw RuntimeException("${pair.moduleName}引用的${it}不存在，请检查工程")
                }
            }
        }
    }
}