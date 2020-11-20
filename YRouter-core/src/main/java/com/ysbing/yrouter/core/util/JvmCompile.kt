package com.ysbing.yrouter.core.util

import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSourceLocation
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.cli.common.messages.MessageRenderer
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
        execImpl(MyPrintingMessageCollector(), Services.EMPTY, args)
    }.code == 0

    private class MyPrintingMessageCollector : MessageCollector {
        private var hasErrors = false
        private val errStream = System.out
        private val messageRenderer = MessageRenderer.WITHOUT_PATHS
        override fun clear() {
        }

        override fun hasErrors() = hasErrors

        override fun report(
            severity: CompilerMessageSeverity,
            message: String,
            location: CompilerMessageSourceLocation?
        ) {
            if (CompilerMessageSeverity.VERBOSE.contains(severity)) {
                return
            }
            hasErrors = hasErrors or severity.isError
            if (hasErrors) {
                errStream.println(messageRenderer.render(severity, message, location));
            }
        }
    }
}