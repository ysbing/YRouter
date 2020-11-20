package com.ysbing.yrouter.core.util

import java.lang.reflect.Type
import kotlin.reflect.KClass

/**
 * Java类转Kotlin
 */
object JavaToKotlinObj {
    private val classMap = mapOf(
        "java.lang.Boolean" to "kotlin.Boolean",
        "java.lang.Character" to "kotlin.Char",
        "java.lang.Byte" to "kotlin.Byte",
        "java.lang.Short" to "kotlin.Short",
        "java.lang.Integer" to "kotlin.Int",
        "java.lang.Float" to "kotlin.Float",
        "java.lang.Long" to "kotlin.Long",
        "java.lang.Double" to "kotlin.Double",
        "java.lang.String" to "kotlin.String",
        "java.lang.Number" to "kotlin.Number",

        "java.lang.Error" to "kotlin.Error",
        "java.lang.Throwable" to "kotlin.Throwable",
        "java.lang.Exception" to "kotlin.Exception",
        "java.lang.RuntimeException" to "kotlin.RuntimeException",
        "java.lang.IllegalArgumentException" to "kotlin.IllegalArgumentException",
        "java.lang.IllegalStateException" to "kotlin.IllegalStateException",
        "java.lang.IndexOutOfBoundsException" to "kotlin.IndexOutOfBoundsException",
        "java.lang.UnsupportedOperationException" to "kotlin.UnsupportedOperationException",
        "java.lang.ArithmeticException" to "kotlin.ArithmeticException",
        "java.lang.NumberFormatException" to "kotlin.NumberFormatException",
        "java.lang.NullPointerException" to "kotlin.NullPointerException",
        "java.lang.ClassCastException" to "kotlin.ClassCastException",
        "java.lang.AssertionError" to "kotlin.AssertionError",
        "java.lang.NoSuchElementException" to "kotlin.NoSuchElementException",
        "java.util.ConcurrentModificationException" to "kotlin.ConcurrentModificationException",

        "java.util.Comparator" to "kotlin.Comparator",
        "java.lang.Iterable" to "kotlin.collections.Iterable",
        "java.util.Collection" to "kotlin.collections.Collection",
        "java.util.List" to "kotlin.collections.List",
        "java.util.Set" to "kotlin.collections.Set",
        "java.util.Map" to "kotlin.collections.Map"
    )

    fun getPrimitiveType(typeName: String): Pair<Type, Boolean> {
        return when (typeName) {
            "boolean" -> {
                Pair(Boolean::class.java, false)
            }
            "java.lang.Boolean" -> {
                Pair(Boolean::class.java, true)
            }
            "char" -> {
                Pair(Char::class.java, false)
            }
            "java.lang.Character" -> {
                Pair(Char::class.java, true)
            }
            "byte" -> {
                Pair(Byte::class.java, false)
            }
            "java.lang.Byte" -> {
                Pair(Byte::class.java, true)
            }
            "short" -> {
                Pair(Short::class.java, false)
            }
            "java.lang.Short" -> {
                Pair(Short::class.java, true)
            }
            "int" -> {
                Pair(Int::class.java, false)
            }
            "java.lang.Integer" -> {
                Pair(Int::class.java, true)
            }
            "float" -> {
                Pair(Float::class.java, false)
            }
            "java.lang.Float" -> {
                Pair(Float::class.java, true)
            }
            "long" -> {
                Pair(Long::class.java, false)
            }
            "java.lang.Long" -> {
                Pair(Long::class.java, true)
            }
            "double" -> {
                Pair(Double::class.java, false)
            }
            "java.lang.Double" -> {
                Pair(Double::class.java, true)
            }
            "void" -> {
                Pair(Void::class.java, false)
            }
            "java.lang.Void" -> {
                Pair(Void::class.java, true)
            }
            String::class.java.name -> {
                Pair(String::class.java, false)
            }
            else -> {
                Pair(Any::class.java, false)
            }
        }
    }

    fun getKotlinPrimitiveType(typeName: String): KClass<*> {
        return when (typeName) {
            "boolean", "java.lang.Boolean" -> {
                Boolean::class
            }
            "char", "java.lang.Character" -> {
                Char::class
            }
            "byte", "java.lang.Byte" -> {
                Byte::class
            }
            "short", "java.lang.Short" -> {
                Short::class
            }
            "int", "java.lang.Integer" -> {
                Int::class
            }
            "float", "java.lang.Float" -> {
                Float::class
            }
            "long", "java.lang.Long" -> {
                Long::class
            }
            "double", "java.lang.Double" -> {
                Double::class
            }
            "void", "java.lang.Void" -> {
                Void::class
            }
            String::class.java.name -> {
                String::class
            }
            else -> {
                Any::class
            }
        }
    }

    fun getPrimitiveArrayType(typeName: String): Type? {
        return when (typeName) {
            "boolean", "java.lang.Boolean" -> {
                BooleanArray::class.java
            }
            "char", "java.lang.Character" -> {
                CharArray::class.java
            }
            "byte", "java.lang.Byte" -> {
                ByteArray::class.java
            }
            "short", "java.lang.Short" -> {
                ShortArray::class.java
            }
            "int", "java.lang.Integer" -> {
                IntArray::class.java
            }
            "float", "java.lang.Float" -> {
                FloatArray::class.java
            }
            "long", "java.lang.Long" -> {
                LongArray::class.java
            }
            "double", "java.lang.Double" -> {
                DoubleArray::class.java
            }
            else -> {
                null
            }
        }
    }

    fun javaToKotlin(javaClassName: String): String {
        return if (classMap.containsKey(javaClassName)) {
            classMap[javaClassName] ?: javaClassName
        } else {
            javaClassName
        }
    }

    fun contains(className: String): Boolean {
        return classMap.containsKey(className) || classMap.containsValue(className)
    }
}