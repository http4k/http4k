package org.http4k.connect.typesafe

import org.http4k.format.MoshiArray
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiNull
import org.http4k.format.MoshiObject
import org.http4k.lens.BiDiMapping

inline fun <reified T : Any> MoshiNode.asA(): T = TypeSafeMoshi.asA(this, T::class)

@PublishedApi
internal fun Any?.asEntry(): MoshiNode = when (this) {
    null -> MoshiNull
    is MoshiNode -> this
    is Iterable<*> -> MoshiArray(map { it.asEntry() })
    is Map<*, *> -> MoshiObject(entries.associate { (key, value) -> key.toString() to value.asEntry() }.toMutableMap())
    else -> TypeSafeMoshi.asJsonObject(this)
}

object Entry {
    inline operator fun <reified T : Any> invoke(): BiDiMapping<MoshiNode, T> =
        BiDiMapping({ node: MoshiNode -> node.asA<T>() }, { value: T -> value.asEntry() })
}

operator fun <T> BiDiMapping<MoshiNode, T>.invoke(nodes: List<MoshiNode>): List<T> = nodes.map { invoke(it) }

operator fun <T> BiDiMapping<MoshiNode, T>.invoke(nodes: Map<String, MoshiNode>): Map<String, T> =
    nodes.mapValues { invoke(it.value) }

inline fun <reified T : Any> Answer.Choice.chosen(question: Question.Choice): T =
    question.criteria.getValue(choice).asA()
