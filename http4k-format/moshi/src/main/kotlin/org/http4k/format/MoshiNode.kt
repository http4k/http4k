package org.http4k.format

sealed interface MoshiNode {
    companion object
}

data class MoshiArray(val elements: List<MoshiNode>): MoshiNode
data class MoshiObject(val attributes: Map<String, MoshiNode>): MoshiNode
data class MoshiString(val value: String): MoshiNode
data class MoshiInteger(val value: Long): MoshiNode
data class MoshiDecimal(val value: Double): MoshiNode
data class MoshiBoolean(val value: Boolean): MoshiNode
object MoshiNull: MoshiNode

fun MoshiNode.unwrap(): Any? = when(this) {
    is MoshiArray -> elements.map { it.unwrap() }
    is MoshiObject -> attributes.mapValues { (_, value) -> value.unwrap() }
    is MoshiString -> value
    is MoshiInteger -> value
    is MoshiDecimal -> value
    is MoshiNull -> null
    is MoshiBoolean -> value
}

fun MoshiNode.Companion.wrap(obj: Any?): MoshiNode = when(obj) {
    null -> MoshiNull
    is Iterable<*> -> obj
        .map { wrap(it) }
        .toList()
        .let { MoshiArray(it) }
    is Map<*, *> -> obj
        .mapKeys { (key, _) -> key.toString() }
        .mapValues { (_, value) -> wrap(value) }
        .let { MoshiObject(it) }
    is Number -> {
        val decimalValue = obj.toString().toBigDecimal()
        if (decimalValue.stripTrailingZeros().scale() <= 0) {
            MoshiInteger(decimalValue.toLong())
        } else {
            MoshiDecimal(decimalValue.toDouble())
        }
    }
    is String -> MoshiString(obj)
    is Boolean -> MoshiBoolean(obj)
    else -> throw IllegalArgumentException("Invalid json value: $obj")
}
