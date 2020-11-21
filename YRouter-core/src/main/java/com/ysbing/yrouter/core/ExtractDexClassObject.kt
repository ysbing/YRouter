package com.ysbing.yrouter.core

import com.ysbing.yrouter.api.YRouterApi
import com.ysbing.yrouter.api.YRouterSkip
import com.ysbing.yrouter.core.util.DecompileUtil
import com.ysbing.yrouter.core.util.DecompileUtil.makeSignature
import com.ysbing.yrouter.core.util.JavaToKotlinObj
import com.ysbing.yrouter.core.util.WriteCodeUtil
import jadx.api.JadxArgs
import jadx.core.dex.attributes.AType
import jadx.core.dex.attributes.annotations.Annotation
import jadx.core.dex.instructions.args.ArgType
import jadx.core.dex.nodes.ClassNode
import jadx.core.dex.nodes.FieldNode
import jadx.core.dex.nodes.MethodNode
import jadx.core.dex.nodes.RootNode
import jadx.core.dex.visitors.DepthTraversal
import jadx.core.dex.visitors.IDexTreeVisitor
import jadx.core.dex.visitors.ModVisitor
import jadx.core.utils.files.InputFile
import java.io.File
import java.lang.reflect.Constructor
import java.lang.reflect.Modifier
import javax.lang.model.SourceVersion


class ExtractDexClassObject(private val infoList: MutableList<DexBean>) {

    companion object {
        fun getClassTypeFromClassNode(classNode: ClassNode): DexBean.ClassType {
            return when {
                classNode.accessFlags.isInterface -> {
                    DexBean.ClassType.INTERFACE
                }
                classNode.isEnum -> {
                    DexBean.ClassType.ENUM
                }
                WriteCodeUtil.isKotlin(classNode) &&
                        classNode.fields.find { it.name == "INSTANCE" } != null &&
                        classNode.fields.find { it.name == "Companion" } == null -> {
                    DexBean.ClassType.OBJECT
                }
                else -> DexBean.ClassType.CLASS
            }
        }
    }

    private val mInterfaceInfo: MutableMap<ClassNode, MutableSet<String>> = HashMap()
    private val mSuperInfo: MutableMap<ClassNode, Pair<List<ArgType>, List<String>>> = HashMap()
    private val rootNode: RootNode = RootNode(JadxArgs().apply {
        isRespectBytecodeAccModifiers = true
    })
    private val loadedInputs = ArrayList<InputFile>()
    private val list = HashSet<DexBean>()

    /**
     * 测试选项，便于调试
     */
    private fun getTestOption(classNode: ClassNode): Boolean {
        return false
    }

    fun load(file: File) {
        println("yrouter begin extract from class:$file")
        InputFile.addFilesFrom(file, loadedInputs, true)
    }

    fun run() {
        rootNode.load(loadedInputs)
        rootNode.initClassPath()
        rootNode.initPasses()
        rootNode.getClasses(false).map { classNode ->
            extract(classNode)
        }
        list.groupBy {
            it.classNode
        }.map {
            it.value.map { bean ->
                rootNode.passes.map { visitor ->
                    visit(visitor, bean)
                }
            }
        }
        infoList.addAll(list)
    }

    private fun visit(visitor: IDexTreeVisitor, bean: DexBean) {
        if (bean.nodeType == DexBean.NodeType.INNER) {
            bean.inner.values.map {
                it.map {
                    visit(visitor, it)
                }
            }
        }
        bean.method?.let { method ->
            method.load()
            if (visitor !is ModVisitor) {
                DepthTraversal.visit(visitor, method)
            }
        }
    }

    private fun extractSuperInfo(
        originalClassNode: ClassNode,
        targetClassNode: ClassNode,
        inner: Boolean = false
    ) {
        if (mInterfaceInfo.containsKey(originalClassNode) && !inner) {
            return
        }
        if (!inner) {
            mInterfaceInfo.getOrPut(originalClassNode) { HashSet() }
        }
        fun extract(argType: ArgType) {
            val searchClassNode = rootNode.searchClassByName(argType.`object`)
            if (searchClassNode == null && argType.`object` != Any::class.java.name &&
                JavaToKotlinObj.getPrimitiveType(argType.`object`).first == Any::class.java
            ) {
                try {
                    val searchClass = Class.forName(argType.`object`)
                    searchClass.fields.map {
                        mInterfaceInfo.getOrPut(originalClassNode) { HashSet() }
                            .add(it.makeSignature())
                    }
                    searchClass.methods.map {
                        mInterfaceInfo.getOrPut(originalClassNode) { HashSet() }
                            .add(it.makeSignature())
                    }
                } catch (e: ClassNotFoundException) {
                }
            } else {
                searchClassNode?.let { interfaceClassNode ->
                    if (interfaceClassNode.isEnum) {
                        return@let
                    }
                    var parentClassNodeApi: Annotation? = null
                    var parentClassNode = interfaceClassNode.parentClass
                    while (parentClassNode != null) {
                        parentClassNodeApi =
                            parentClassNode.getAnnotation(YRouterApi::class.java.name)
                        if (parentClassNodeApi != null) {
                            break
                        }
                        if (parentClassNode == parentClassNode.parentClass) {
                            break
                        }
                        parentClassNode = parentClassNode.parentClass
                    }
                    interfaceClassNode.fields.map {
                        if (!it.accessFlags.isPrivate && !it.accessFlags.isBridge && !it.accessFlags.isSynthetic
                            && (parentClassNodeApi != null
                                    || interfaceClassNode.getAnnotation(YRouterApi::class.java.name) != null
                                    || it.getAnnotation(YRouterApi::class.java.name) != null
                                    || getTestOption(interfaceClassNode))
                        ) {
                            mInterfaceInfo.getOrPut(originalClassNode) { HashSet() }
                                .add(it.makeSignature())
                        }
                    }
                    interfaceClassNode.methods.map {
                        if (!it.accessFlags.isPrivate && !it.accessFlags.isBridge && !it.accessFlags.isSynthetic
                            && (parentClassNodeApi != null
                                    || interfaceClassNode.getAnnotation(YRouterApi::class.java.name) != null
                                    || it.getAnnotation(YRouterApi::class.java.name) != null
                                    || getTestOption(interfaceClassNode))
                        ) {
                            mInterfaceInfo.getOrPut(originalClassNode) { HashSet() }
                                .add(it.makeSignature())
                        }
                    }
                    extractSuperInfo(originalClassNode, interfaceClassNode, true)
                }
            }
        }
        targetClassNode.interfaces?.map {
            extract(it)
        }
        targetClassNode.superClass?.let {
            extract(it)
        }
    }

    /**
     * 比较该方法和要引用的类全部继承的方法是否有相同
     */
    private fun classInfoSignatureEquals(
        argType: ArgType?,
        signature: String
    ): Boolean {
        val searchClassNode = rootNode.searchClassByName(argType?.`object`) ?: return false
        searchClassNode.superClass?.let {
            if (classInfoSignatureEquals(it, signature)) {
                return true
            }
        }
        searchClassNode.methods.map {
            if (DecompileUtil.signatureEquals(it.makeSignature(), signature)
                && checkMethod(it)
                && (searchClassNode.getAnnotation(YRouterApi::class.java.name) != null
                        || it.getAnnotation(YRouterApi::class.java.name) != null
                        || getTestOption(searchClassNode))
            ) {
                return true
            }
        }
        return false
    }

    private fun extractClass(classNode: ClassNode) {
        //如果是内部类，需要往上遍历父类导出
        if (classNode.parentClass != null && classNode.parentClass != classNode) {
            extractClass(classNode.parentClass)
        }
        //必须加上一个构造函数
        classNode.methods.find { it.isConstructor }?.let {
            if (checkMethod(it)) {
                extractMethod(classNode, it)
            }
        }
        val parentClassNode = if (classNode.classInfo.isInner) classNode.parentClass else null
        val parentDexBean = find(list, parentClassNode)
        val dexBean = DexBean()
        dexBean.classNode = classNode
        dexBean.classType = getClassTypeFromClassNode(classNode)
        dexBean.nodeType = DexBean.NodeType.INNER
        if (parentDexBean == null) {
            list.add(dexBean)
        } else {
            parentDexBean.inner?.getOrPut(classNode) { HashSet() }?.add(dexBean)
        }
    }

    private fun extractInterfaceMethod(classNode: ClassNode) {
        val map = HashMap<ClassNode, MutableSet<MethodNode>>()
        extractSuperMethod(classNode, map)
        classNode.interfaces.map { argType ->
            fun wrapMethods(argType: ArgType) {
                rootNode.searchClassByName(argType.`object`)?.let { interfaceClassNode ->
                    interfaceClassNode.interfaces?.map { wrapMethods(it) }
                    interfaceClassNode.methods.map { methodNode ->
                        if (classNode.methods.find { classNodeFind ->
                                checkMethod(classNodeFind) && DecompileUtil.signatureEquals(
                                    classNodeFind.makeSignature(),
                                    methodNode.makeSignature()
                                )
                            } == null) {
                            var has = false
                            map.keys.map methodClassNode@{ methodClassNode ->
                                val findMethodNode = map[methodClassNode]?.find { find ->
                                    checkMethod(find) && DecompileUtil.signatureEquals(
                                        methodNode.makeSignature(false),
                                        find.makeSignature(false)
                                    )
                                }
                                if (findMethodNode != null) {
                                    has = true
                                    if (methodClassNode.getAnnotation(YRouterApi::class.java.name) == null
                                        && findMethodNode.getAnnotation(YRouterApi::class.java.name) == null
                                        && checkMethod(findMethodNode)
                                    ) {
                                        var superClassNode: ArgType? = classNode.superClass
                                        while (superClassNode != null) {
                                            superClassNode.let { argType ->
                                                val searchClass =
                                                    rootNode.searchClassByName(argType.`object`)
                                                searchClass?.let { it1 -> extractClass(it1) }
                                                superClassNode = searchClass?.superClass
                                                if (superClassNode == methodClassNode) {
                                                    superClassNode = null
                                                }
                                            }
                                        }
                                        extractMethod(methodClassNode, findMethodNode)
                                    }
                                    return@methodClassNode
                                }
                            }
                            if (!has && !classNode.accessFlags.isInterface
                                && !classNode.accessFlags.isAbstract
                                && !classNode.accessFlags.isEnum
                                && !classInfoSignatureEquals(
                                    classNode.superClass,
                                    methodNode.makeSignature()
                                )
                                && checkMethod(methodNode)
                            ) {
                                extractMethod(classNode, methodNode)
                            }
                        }
                    }
                }
            }
            wrapMethods(argType)
        }
    }

    private fun extractSuperMethod(
        classNode: ClassNode,
        map: MutableMap<ClassNode, MutableSet<MethodNode>>
    ) {
        classNode.methods.map {
            var parentClassNode = classNode.parentClass
            while (parentClassNode != null) {
                if (parentClassNode == parentClassNode.parentClass) {
                    break
                }
                parentClassNode = parentClassNode.parentClass
            }
            if (!it.accessFlags.isPrivate && !it.accessFlags.isFinal && checkMethod(it)
            ) {
                map.getOrPut(classNode) { HashSet() }.add(it)
            }
        }
        classNode.superClass?.let { argType ->
            rootNode.searchClassByName(argType.`object`)?.let { superClassNode ->
                extractSuperMethod(superClassNode, map)
            }
        }
    }

    private fun extractField(
        classNode: ClassNode,
        field: FieldNode
    ) {
        println("yrouter extractField:$field")
        extractSuperInfo(classNode, classNode)
        val dexBean = DexBean()
        dexBean.classNode = classNode
        dexBean.classType = getClassTypeFromClassNode(classNode)
        dexBean.nodeType = DexBean.NodeType.FIELD
        dexBean.field = field
        dexBean.interfaceInfo = mInterfaceInfo[classNode]
        addToList(dexBean, classNode)
    }

    private fun extractMethod(
        classNode: ClassNode,
        method: MethodNode
    ) {
        println("yrouter extractMethod:$method")
        extractSuperInfo(classNode, classNode)
        val dexBean = DexBean()
        dexBean.classNode = classNode
        dexBean.classType = getClassTypeFromClassNode(classNode)
        dexBean.nodeType = DexBean.NodeType.METHOD
        dexBean.method = method
        dexBean.interfaceInfo = mInterfaceInfo[classNode]
        dexBean.superInfo1 = getSuperInfo(classNode, method)?.first
        dexBean.superInfo2 = getSuperInfo(classNode, method)?.second
        addToList(dexBean, classNode)
    }

    private fun getSuperInfo(
        classNode: ClassNode,
        method: MethodNode
    ): Pair<List<ArgType>, List<String>>? {
        if (!method.isConstructor || classNode.superClass?.`object` == Any::class.java.name) {
            return null
        }
        if (mSuperInfo.containsKey(classNode)) {
            return mSuperInfo[classNode]
        }
        val returnValue1 = arrayListOf<ArgType>()
        val returnValue2 = arrayListOf<String>()
        val superClassNode = rootNode.searchClassByName(classNode.superClass?.`object`)
        if (superClassNode == null) {
            try {
                val constructorArray = arrayListOf<Pair<Constructor<*>, Int>>()
                Class.forName(classNode.superClass?.`object`).declaredConstructors.map {
                    val level = when {
                        it.modifiers and Modifier.PUBLIC != 0 -> {
                            2
                        }
                        it.modifiers and Modifier.PROTECTED != 0 -> {
                            1
                        }
                        it.modifiers and (Modifier.PUBLIC or Modifier.PRIVATE or Modifier.PROTECTED) == 0 -> {
                            0
                        }
                        else -> {
                            return@map
                        }
                    }
                    if (it.parameterCount == 0) {
                        return null
                    }
                    constructorArray.add(Pair(it, level))
                }
                constructorArray.maxByOrNull { it.second }?.first?.parameters?.map {
                    returnValue2.add(
                        it.parameterizedType.typeName
                    )
                }
            } catch (e: ClassNotFoundException) {
            }
        } else {
            val constructorArray = arrayListOf<Pair<MethodNode, Int>>()
            superClassNode.methods?.map {
                if (it.isConstructor && !it.accessFlags.isSynthetic) {
                    val level = when {
                        it.accessFlags.isPublic -> {
                            2
                        }
                        it.accessFlags.isProtected -> {
                            1
                        }
                        it.accessFlags.isPackagePrivate -> {
                            0
                        }
                        else -> {
                            return@map
                        }
                    }
                    if (it.argTypes.isEmpty()) {
                        return null
                    }
                    constructorArray.add(Pair(it, level))
                }
            }
            constructorArray.maxByOrNull { it.second }
                ?.apply { first.load() }?.first?.apply {
                    extractClass(superClassNode)
                    extractMethod(superClassNode, this)
                }?.argRegs?.map {
                    val argType =
                        if (it.type != ArgType.UNKNOWN) it.type else it.initType
                    returnValue1.add(argType)
                }
        }
        mSuperInfo[classNode] = Pair(returnValue1, returnValue2)
        return mSuperInfo[classNode]
    }

    private fun find(list: MutableSet<DexBean>, parent: ClassNode?): DexBean? {
        list.map { find ->
            val result =
                find.classNode == parent && find.nodeType == DexBean.NodeType.INNER
            if (result) {
                return find
            }
            find.inner.values.map {
                val classResult = find(it, parent)
                if (classResult != null) {
                    return classResult
                }
            }
        }
        return null
    }

    private fun addToList(
        dexBean: DexBean,
        classNode: ClassNode,
    ) {
        val parentClassNode = if (classNode.classInfo.isInner) classNode.parentClass else null
        val parentDexBean = find(list, classNode)
        var dexInnerBean: DexBean? = null
        if (parentDexBean == null) {
            dexInnerBean = DexBean()
            dexInnerBean.classNode = classNode
            dexInnerBean.classType = getClassTypeFromClassNode(classNode)
            dexInnerBean.nodeType = DexBean.NodeType.INNER
        }
        if (parentClassNode == null) {
            list.add(dexBean)
        } else {
            val bean = find(list, parentClassNode)
            if (bean == null) {
                list.add(dexBean)
                dexInnerBean?.let {
                    list.add(it)
                }
            } else {
                bean.inner?.getOrPut(classNode) { HashSet() }?.add(dexBean)
                dexInnerBean?.let {
                    bean.inner?.getOrPut(classNode) { HashSet() }?.add(it)
                }
            }
        }
    }

    private fun extract(
        classNode: ClassNode,
        extractClass: Boolean = false
    ) {
        if (checkClass(classNode)) {
            if (classNode.getAnnotation(YRouterApi::class.java.name) != null
                || extractClass
                || getTestOption(classNode)
            ) {
                extractClass(classNode)
                extractInterfaceMethod(classNode)
                classNode.fields.map annotation@{ field ->
                    if (field.getAnnotation(YRouterSkip::class.java.name) == null
                        && checkField(field)
                    ) {
                        extractField(classNode, field)
                    }
                }
                classNode.methods.map annotation@{ method ->
                    if (method.getAnnotation(YRouterSkip::class.java.name) == null
                        && checkMethod(method)
                    ) {
                        extractMethod(classNode, method)
                    }
                }
                classNode.innerClasses.map { innerClass ->
                    if (checkClass(innerClass)) {
                        extract(innerClass, true)
                    }
                }
            } else {
                classNode.fields.map annotation@{ field ->
                    if (field.getAnnotation(YRouterApi::class.java.name) != null && checkField(field)) {
                        extractClass(classNode)
                        extractField(classNode, field)
                    }
                }
                classNode.methods.map annotation@{ method ->
                    if (method.getAnnotation(YRouterApi::class.java.name) != null
                        && checkMethod(method)
                    ) {
                        extractClass(classNode)
                        extractMethod(classNode, method)
                    }
                }
                classNode.innerClasses.map { innerClass ->
                    if (checkClass(innerClass)) {
                        extract(innerClass)
                    }
                }
            }
        }
    }

    private fun checkClass(classNode: ClassNode): Boolean {
        return SourceVersion.isName(classNode.classInfo.shortName)
                && !classNode.`package`.startsWith("kotlin.")
                && classNode.shortName != "DefaultImpls"
                && !classNode.shortName.contains("\\$\\d".toRegex())
                && classNode.interfaces.find { it.toString() == "android.os.Parcelable\$Creator" } == null
    }

    private fun checkField(fieldNode: FieldNode): Boolean {
        return SourceVersion.isName(fieldNode.parentClass.classInfo.shortName)
                && fieldNode.name != "INSTANCE"
                && fieldNode.name != "Companion"
                && !fieldNode.name.contains("\$")
                && fieldNode.type.toString() != "android.os.Parcelable\$Creator"
                && !fieldNode.type.toString().contains("\\$\\d".toRegex())
                && fieldNode.parentClass.classInfo.shortName != "DefaultImpls"
                && !fieldNode.parentClass.classInfo.shortName.contains("AnonymousClass")
                && (!WriteCodeUtil.isKotlin(fieldNode.parentClass)
                || fieldNode.parentClass.innerClasses.find { it.shortName == fieldNode.name } == null)
                && fieldNode.parentClass.innerClasses.find { it.shortName == "Companion" }
            ?.fields?.find { it.fieldInfo.type == fieldNode.fieldInfo.type && it.fieldInfo.name == fieldNode.fieldInfo.name } == null
    }

    private fun checkMethod(methodNode: MethodNode): Boolean {
        var newMethodName = methodNode.name
        if (newMethodName.length >= 4 && newMethodName.startsWith("set") && newMethodName[3].isUpperCase()) {
            newMethodName = newMethodName.replace("set", "")
        } else if (newMethodName.length >= 4 && newMethodName.startsWith("get") && newMethodName[3].isUpperCase()) {
            newMethodName = newMethodName.replace("get", "")
        } else if (newMethodName.length >= 3 && newMethodName.startsWith("is") && newMethodName[2].isUpperCase()) {
            newMethodName = newMethodName.replace("is", "")
        }
        return SourceVersion.isName(methodNode.parentClass.classInfo.shortName)
                && !methodNode.methodInfo.isClassInit
                && methodNode.name != "equals"
                && methodNode.name != "hashCode"
                && methodNode.name != "toString"
                && !methodNode.parentClass.isEnum
                && !methodNode.name.contains("\$")
                && !methodNode.argTypes.toString().contains("\\$\\d".toRegex())
                && !methodNode.returnType.toString().contains("\\$\\d".toRegex())
                && !methodNode.accessFlags.isBridge
                && !methodNode.accessFlags.isSynthetic
                && !methodNode.parentClass.classInfo.shortName.contains("AnonymousClass")
                && methodNode.parentClass.classInfo.shortName != "DefaultImpls"
                && methodNode.get(AType.RENAME_REASON) == null
                && (methodNode.parentClass.methods.find {
            if (it == methodNode || it.accessFlags.isBridge || it.accessFlags.isSynthetic) {
                false
            } else {
                DecompileUtil.signatureEquals(it.makeSignature(), methodNode.makeSignature())
            }
        } == null)
                && (newMethodName == methodNode.name || !WriteCodeUtil.isKotlin(methodNode.parentClass) ||
                methodNode.parentClass.fields.find {
                    var fieldName = it.name
                    if (fieldName.length >= 3 && fieldName.startsWith("is") && fieldName[2].isUpperCase()) {
                        fieldName = fieldName.replace("is", "")
                    }
                    fieldName.decapitalize() == newMethodName.decapitalize()
                } == null)
    }
}