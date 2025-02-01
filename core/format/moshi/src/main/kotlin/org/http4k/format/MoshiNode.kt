package org.http4k.format

sealed interface MoshiNode {
    companion object
}

data class MoshiArray(val elements: List<MoshiNode>) : MoshiNode
data class MoshiObject(val attributes: MutableMap<String, MoshiNode>) : MoshiNode

data class MoshiString(val value: String) : MoshiNode
data class MoshiInteger(val value: Int) : MoshiNode
data class MoshiLong(val value: Long) : MoshiNode
data class MoshiDecimal(val value: Double) : MoshiNode
data class MoshiBoolean(val value: Boolean) : MoshiNode
data object MoshiNull : MoshiNode

fun MoshiNode.unwrap(): Any? = when (this) {
    is MoshiArray -> elements.map(MoshiNode::unwrap)
    is MoshiObject -> attributes.mapValues { (_, value) -> value.unwrap() }
    is MoshiString -> value
    is MoshiInteger -> value
    is MoshiLong -> value
    is MoshiDecimal -> value
    is MoshiNull -> null
    is MoshiBoolean -> value
}

fun MoshiNode.Companion.wrap(obj: Any?): MoshiNode = when (obj) {
    null -> MoshiNull
    is Iterable<*> -> obj
        .map { wrap(it) }
        .toList()
        .let { MoshiArray(it) }

    is Map<*, *> -> obj
        .mapKeys { (key, _) -> key.toString() }
        .mapValues { (_, value) -> wrap(value) }
        .let { MoshiObject(it.toMutableMap()) }

    is Number -> when {
        obj is Double && obj.isSafeToConvertToInt() -> MoshiInteger(obj.toInt())
        obj is Double && obj.isSafeToConvertToLong() -> MoshiLong(obj.toLong())
        obj is Long && obj.isSafeToConvertToInt() -> MoshiInteger(obj.toInt())
        obj is Long -> MoshiLong(obj.toLong())
        obj is Int || obj is Short -> MoshiInteger(obj.toInt())
        else -> MoshiDecimal(obj.toDouble())
    }

    is String -> MoshiString(obj)
    is Boolean -> MoshiBoolean(obj)
    else -> throw IllegalArgumentException("Invalid json value: $obj")
}

private fun Double.isSafeToConvertToLong() =
    this % 1.0 == 0.0 && this in Long.MIN_VALUE.toDouble()..Long.MAX_VALUE.toDouble()

private fun Double.isSafeToConvertToInt() =
    this % 1.0 == 0.0 && this in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble()

private fun Long.isSafeToConvertToInt() =
    this in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()
