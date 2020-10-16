package com.ysbing.yrouter.cli

import com.ysbing.yrouter.core.ExtractDexClassObject
import com.ysbing.yrouter.core.DexBean
import com.ysbing.yrouter.core.util.FileOperation
import com.ysbing.yrouter.core.util.MakeJarUtil
import com.ysbing.yrouter.core.util.WriteCodeUtil
import java.io.File
import java.io.PrintStream
import kotlin.system.exitProcess


class CliMain {
    fun run(args: Array<String>) {
//        val dir = "D:\\dev\\ysbing\\YRouter\\tool_output\\"
//        val args = arrayOf(
//            dir + "input.apk",
//            ARG_OUT,
//            dir + "output"
//        )
        if (args.isEmpty()) {
            goToError()
        }

        val readArgs = ReadArgs(args).invoke()
        if (readArgs.outputPath == null) {
            goToError()
        }
        val startTime = System.currentTimeMillis()
        //解压apk
        FileOperation.unZipAPk(readArgs.apkFilePath, readArgs.outputPath)
        val file = File(readArgs.outputPath!!)
        val infoList = ArrayList<DexBean>()
        //收集变量和方法
        file.listFiles { _, name ->
            name?.endsWith(".dex") ?: false
        }?.map {
            ExtractDexClassObject.run(it, infoList)
        }
        val writeCodeUtil = WriteCodeUtil(readArgs.outputPath + "/tmp/src")
        //生成java和kotlin源代码
        infoList.groupBy {
            it.classNode
        }.map {
            writeCodeUtil.run(it.key, it.value)
        }
        //编译源代码
        MakeJarUtil.buildJavaClass(
            readArgs.outputPath + "/tmp/src/lib",
            readArgs.outputPath + "/tmp/src/lib",
            arrayOf()
        )
        MakeJarUtil.buildKotlinClass(
            readArgs.outputPath + "/tmp/src/lib",
            readArgs.outputPath + "/tmp/src/lib",
            arrayOf()
        )
        MakeJarUtil.buildJavaClass(
            readArgs.outputPath + "/tmp/src/main",
            readArgs.outputPath + "/tmp/src/main",
            arrayOf(readArgs.outputPath + "/tmp/src/lib")
        )
        MakeJarUtil.buildKotlinClass(
            readArgs.outputPath + "/tmp/src/main",
            readArgs.outputPath + "/tmp/src/main",
            arrayOf(readArgs.outputPath + "/tmp/src/lib")
        )
        //将所有的class合jar
        val outputFile = File(readArgs.outputPath + "/yrouter_index.jar")
        MakeJarUtil.buildJar(
            File(readArgs.outputPath + "/tmp/src/main"),
            outputFile
        )
        println("yrouter build success->${outputFile.absolutePath}，耗时:${System.currentTimeMillis() - startTime}")
    }

    private inner class ReadArgs(private val args: Array<String>) {
        private var outputFile: File? = null
        var apkFilePath: String? = null
        val outputPath: String?
            get() = outputFile?.path

        operator fun invoke(): ReadArgs {
            var index = 0
            while (index < args.size) {
                val arg = args[index]
                when (arg) {
                    ARG_OUT -> {
                        if (index == args.size - 1) {
                            System.err.println("Missing output file argument")
                            goToError()
                        }
                        outputFile = File(args[++index])
                        val parent = outputFile!!.parentFile
                        if (parent != null && !parent.exists()) {
                            parent.mkdirs()
                        }
                        System.out.printf(
                            "special output directory path: %s\n",
                            outputFile!!.absolutePath
                        )
                    }
                    else -> apkFilePath = arg
                }
                index++
            }
            return this
        }
    }

    private fun goToError() {
        printUsage(System.err)
        exitProcess(0)
    }

    private fun printUsage(out: PrintStream) {
        val command = "yrouter.jar"
        out.println()
        out.println()
        out.println("Usage: java -jar $command input.apk")
        out.println("if you want to special the output path or config file path, you can input:")
    }

    companion object {
        private const val ARG_OUT = "-out"
    }
}