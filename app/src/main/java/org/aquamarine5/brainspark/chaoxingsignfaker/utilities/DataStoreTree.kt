/*
 * Copyright (c) 2026, @aquamarine5 (@海蓝色的咕咕鸽). All Rights Reserved.
 * Author: aquamarine5@163.com (Github: https://github.com/aquamarine5) and Brainspark (previously RenegadeCreation)
 * Repository: https://github.com/aquamarine5/ChaoxingSignFaker
 */

package org.aquamarine5.brainspark.chaoxingsignfaker.utilities

import com.google.protobuf.ByteString
import com.google.protobuf.MessageLite
import java.lang.Deprecated
import java.lang.reflect.Method
import java.lang.reflect.Modifier
import kotlin.Any
import kotlin.Boolean
import kotlin.Double
import kotlin.Enum
import kotlin.Float
import kotlin.Int
import kotlin.Long
import kotlin.String
import kotlin.let
import kotlin.runCatching
import kotlin.toRawBits
import kotlin.toString

sealed interface DataStoreTreeNode {
    val title: String

    data class Value(
        override val title: String,
        val value: String,
        val isDeprecated: Boolean = false
    ) : DataStoreTreeNode

    data class Group(override val title: String, val children: List<DataStoreTreeNode>) :
        DataStoreTreeNode
}

private const val LIST_SUFFIX = "List"
private const val OR_BUILDER_LIST_SUFFIX = "OrBuilderList"
private const val MAP_SUFFIX = "Map"
private const val BYTES_SUFFIX = "Bytes"

fun buildDataStoreTree(message: MessageLite): List<DataStoreTreeNode> {
    val setters = mutableSetOf<String>()
    val hazzers = mutableMapOf<String, Method>()
    val getters = sortedMapOf<String, Method>()
    message.javaClass.declaredMethods.forEach { method ->
        if (Modifier.isStatic(method.modifiers) || method.name.length < 3) return@forEach
        if (method.name.startsWith("set")) {
            setters.add(method.name)
            return@forEach
        }
        if (!Modifier.isPublic(method.modifiers) || method.parameterTypes.isNotEmpty()) return@forEach
        if (method.name.startsWith("has")) hazzers[method.name] = method
        else if (method.name.startsWith("get")) getters[method.name] = method
    }
    val nodes = mutableListOf<DataStoreTreeNode>()
    getters.forEach { (getterName, getter) ->
        val suffix = getterName.substring(3)
        val value = invokeGetter(getter, message) ?: return@forEach
        when {
            suffix.endsWith(LIST_SUFFIX) &&
                    !suffix.endsWith(OR_BUILDER_LIST_SUFFIX) &&
                    suffix != LIST_SUFFIX &&
                    value is List<*> -> {
                val name = fieldName(suffix.dropLast(LIST_SUFFIX.length))
                val children = value.mapIndexed { index, element ->
                    element.toDataStoreTreeNode(index.toString(), isElement = true)
                }
                nodes.add(
                    if (children.isEmpty()) DataStoreTreeNode.Value(name, "[]")
                    else DataStoreTreeNode.Group("$name[${children.size}]", children)
                )
            }

            suffix.endsWith(MAP_SUFFIX) &&
                    suffix != MAP_SUFFIX &&
                    !getter.isAnnotationPresent(Deprecated::class.java) &&
                    value is Map<*, *> -> {
                val name = fieldName(suffix.dropLast(MAP_SUFFIX.length))
                val children = value.entries.map { (key, entryValue) ->
                    entryValue.toDataStoreTreeNode(key.toString(), isElement = true)
                }
                nodes.add(
                    if (children.isEmpty()) DataStoreTreeNode.Value(name, "{}")
                    else DataStoreTreeNode.Group("$name[${children.size}]", children)
                )
            }

            setters.contains("set$suffix") &&
                    !(suffix.endsWith(BYTES_SUFFIX) &&
                            getters.containsKey("get${suffix.dropLast(BYTES_SUFFIX.length)}")) -> {
                val isPresent = hazzers["has$suffix"]?.let { invokeGetter(it, message) as? Boolean }
                    ?: !isDefaultValue(value)
                if (isPresent) nodes.add(
                    value.toDataStoreTreeNode(
                        fieldName(suffix),
                        isDeprecated = getter.isAnnotationPresent(Deprecated::class.java)
                    )
                )
            }
        }
    }
    return nodes
}

private fun Any?.toDataStoreTreeNode(
    title: String,
    isElement: Boolean = false,
    isDeprecated: Boolean = false
): DataStoreTreeNode =
    if (this is MessageLite) {
        val children = buildDataStoreTree(this)
        when {
            children.isEmpty() -> DataStoreTreeNode.Value(title, "{}")
            isElement -> DataStoreTreeNode.Group(title, children)
            else -> DataStoreTreeNode.Group("$title {...}", children)
        }
    } else {
        DataStoreTreeNode.Value(title, this.toString(), isDeprecated)
    }

private fun fieldName(suffix: String): String = suffix.replaceFirstChar { it.lowercaseChar() }

private fun invokeGetter(method: Method, target: Any): Any? =
    runCatching { method.invoke(target) }.getOrNull()

private fun isDefaultValue(value: Any?): Boolean = when (value) {
    is Boolean -> !value
    is Int -> value == 0
    is Long -> value == 0L
    is Float -> value.toRawBits() == 0
    is Double -> value.toRawBits() == 0L
    is String -> value.isEmpty()
    is ByteString -> value == ByteString.EMPTY
    is MessageLite -> value == value.defaultInstanceForType
    is Enum<*> -> value.ordinal == 0
    else -> false
}
