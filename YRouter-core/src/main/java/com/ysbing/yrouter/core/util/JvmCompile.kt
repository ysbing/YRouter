package com.ysbing.yrouter.core.util

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.com.intellij.openapi.util.SystemInfo
import org.jetbrains.kotlin.config.Services
import java.io.File

object JvmCompile {

    fun run(input: File, output: File, buildTools: Array<String>): Boolean = K2JVMCompiler().run {
        val args = K2JVMCompilerArguments().apply {
            freeArgs = listOf(input.absolutePath)
            destination = output.absolutePath
            classpath = System.getProperty("java.class.path")
            for (buildTool in buildTools) {
                classpath += if (SystemInfo.isWindows) {
                    ";"
                } else {
                    ":"
                }
                classpath += buildTool
            }
            noStdlib = true
            noReflect = true
            skipRuntimeVersionCheck = true
            reportPerf = false
        }
//        execImpl(
//            PrintingMessageCollector(
//                System.out,
//                MessageRenderer.WITHOUT_PATHS, false
//            ), Services.EMPTY, args
//        )
        execImpl(MessageCollector.NONE, Services.EMPTY, args)
    }.code == 0
}