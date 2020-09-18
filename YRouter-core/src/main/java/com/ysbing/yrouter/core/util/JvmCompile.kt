package com.ysbing.yrouter.core.util

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.jvm.K2JVMCompiler
import org.jetbrains.kotlin.config.Services
import java.io.File

object JvmCompile {

    fun exe(input: File, output: File, buildTools: Array<String>): Boolean = K2JVMCompiler().run {
        val args = K2JVMCompilerArguments().apply {
            freeArgs = listOf(input.absolutePath)
            destination = output.absolutePath
            var buildToolStrBuilder = StringBuilder()
            for (buildTool in buildTools) {
                buildToolStrBuilder.append(buildTool).append(";")
            }
            if (buildToolStrBuilder.toString().endsWith(";")) {
                buildToolStrBuilder =
                    StringBuilder(buildToolStrBuilder.substring(0, buildToolStrBuilder.length - 1))
            }
            val buildToolStr = buildToolStrBuilder.toString()
            if (buildToolStr.isNotEmpty()) {
                classpath = buildToolStr
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