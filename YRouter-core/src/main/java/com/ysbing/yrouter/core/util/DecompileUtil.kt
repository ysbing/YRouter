package com.ysbing.yrouter.core.util

import com.squareup.kotlinpoet.ENUM
import jadx.api.JadxArgs
import jadx.core.codegen.TypeGen
import jadx.core.dex.instructions.args.ArgType
import jadx.core.dex.nodes.ClassNode
import jadx.core.dex.nodes.FieldNode
import jadx.core.dex.nodes.MethodNode
import jadx.core.dex.nodes.RootNode
import jadx.core.utils.files.InputFile
import java.io.File
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashSet

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
            if (it.exists()) {
                InputFile.addFilesFrom(it, loadedInputs, true)
            }
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

    fun Field.makeSignature(): String {
        return "${this.name}:${TypeGen.signature(ArgType.`object`(this.type.name))}"
    }

    fun FieldNode.makeSignature(): String {
        return "${this.fieldInfo.name}:${TypeGen.signature(this.fieldInfo.type)}"
    }

    fun Method.makeSignature(): String {
        val signature = StringBuilder()
        signature.append(this.name)
        val getGenericSignatureMethod = Method::class.java.getDeclaredMethod("getGenericSignature")
        getGenericSignatureMethod.isAccessible = true
        val methodSignature = getGenericSignatureMethod.invoke(this)
        if (methodSignature != null) {
            signature.append(methodSignature)
        } else {
            signature.append('(')
            this.parameterTypes.map {
                val arg = getArgType(it)
                val sign = TypeGen.signature(arg)
                signature.append(sign)
                if (!sign.endsWith(";")) {
                    signature.append(";")
                }
            }
            signature.append(')')
            signature.append(TypeGen.signature(getArgType(this.returnType)))
        }
        return signature.toString()
    }

    fun MethodNode.makeSignature(hasReturn: Boolean = true): String {
        this.load()
        val signature = StringBuilder()
        signature.append(this.name)
        signature.append("(")
        this.argRegs.map { arg ->
            val argType =
                if (arg.type != ArgType.UNKNOWN) arg.type else arg.initType
            val sign = TypeGen.signature(argType)
            signature.append(sign)
            if (!sign.endsWith(";")) {
                signature.append(";")
            }
        }
        signature.append(")")
        if (hasReturn) {
            signature.append(TypeGen.signature(this.returnType))
        }
        return signature.toString()
    }

    fun methodSupportContains(superInfo: Set<String>?, methodSignature: String): Boolean {
        return superInfo?.find { signatureEquals(it, methodSignature) } != null
    }

    fun signatureEquals(signature1: String, signature2: String): Boolean {
        if (signature1 == signature2) {
            return true
        }
        val name =
            signature1.substringBefore("(") == signature2.substringBefore("(")
        if (!name) {
            return false
        }
        val args1 = signature1.substringAfter("(").substringBeforeLast(")").split(";")
        val args2 = signature2.substringAfter("(").substringBeforeLast(")")
            .split(";")
        if (args1.size != args2.size) {
            return false
        }
        args1.mapIndexed { index, s1 ->
            val s2 = args2[index]
            if (isGeneric(s1) || isGeneric(s2)) {
                return@mapIndexed
            }
            if (s1 != s2) {
                return false
            }
        }
        val ret1 = signature1.substringAfterLast(")")
        val ret2 = signature2.substringAfterLast(")")
        if (!isGeneric(ret1) && !isGeneric(ret1) && ret1 != ret2) {
            return false
        }
        return true
    }

    private fun isGeneric(type: String): Boolean {
        if (type.startsWith("T")) {
            return true
        }
        if (type.contains("/") || !type.contains("L")) {
            return false
        }
        return true
    }

    private fun getArgType(clazz: Class<*>): ArgType {
        if (clazz.name == "void") {
            return ArgType.VOID
        }
        return when (clazz) {
            Int::class.java -> {
                ArgType.INT
            }
            Boolean::class.java -> {
                ArgType.BOOLEAN
            }
            Byte::class.java -> {
                ArgType.BYTE
            }
            Short::class.java -> {
                ArgType.SHORT
            }
            Char::class.java -> {
                ArgType.CHAR
            }
            Float::class.java -> {
                ArgType.FLOAT
            }
            Double::class.java -> {
                ArgType.DOUBLE
            }
            Long::class.java -> {
                ArgType.LONG
            }
            Void::class.java -> {
                ArgType.VOID
            }
            Any::class.java -> {
                ArgType.OBJECT
            }
            Class::class.java -> {
                ArgType.CLASS
            }
            String::class.java -> {
                ArgType.STRING
            }
            ENUM::class.java -> {
                ArgType.ENUM
            }
            else -> {
                ArgType.`object`(clazz.name)
            }
        }
    }
}