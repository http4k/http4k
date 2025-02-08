package dev.forkhandles.data

import org.http4k.format.MoshiArray
import org.http4k.format.MoshiBoolean
import org.http4k.format.MoshiDecimal
import org.http4k.format.MoshiInteger
import org.http4k.format.MoshiLong
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiNull
import org.http4k.format.MoshiObject
import org.http4k.format.MoshiString
import java.math.BigDecimal
import java.math.BigInteger

/**
 * Moshi MoshiNode-based implementation of the DataContainer
 */
open class MoshiNodeDataContainer(input: MoshiNode) :
    DataContainer<MoshiNode>(
        input,
        { content, it -> (content as? MoshiObject)?.attributes?.containsKey(it) ?: false },
        { content, it ->
            (content as? MoshiObject)?.attributes?.get(it)?.let { nodeToValue(it) }
        },
        { node, name, value ->
            (node as? MoshiObject)?.also { it.attributes[name] = value.toNode() }
                ?: error("Invalid node type ${input::class.java}")
        }
    ) {

    companion object {
        private fun Any?.toNode(): MoshiNode = when (this) {
            null -> MoshiNull
            is MoshiNode -> this
            is DataContainer<*> -> unwrap().toNode()
            is Boolean -> MoshiBoolean(this)
            is Short -> MoshiInteger(toInt())
            is Int -> MoshiInteger(toInt())
            is Long -> MoshiLong(this)
            is BigInteger -> when {
                canConvertToInt() -> MoshiInteger(toInt())
                canConvertToLong() -> MoshiLong(toLong())
                else -> MoshiString(toString())
            }

            is Double -> MoshiDecimal(this)
            is Float -> MoshiDecimal(toDouble())
            is BigDecimal -> when (canConvertToDouble()) {
                true -> MoshiDecimal(toDouble())
                else -> MoshiString(toString())
            }

            is String -> MoshiString(this)
            is Iterable<*> -> MoshiArray(map { it.toNode() })

            else -> error("Invalid node type ${this::class.java}")
        }

        private fun nodeToValue(input: MoshiNode): Any? = when (input) {
            is MoshiNull -> null
            is MoshiString -> input.value
            is MoshiArray -> input.elements.map(::nodeToValue)
            is MoshiObject -> input
            is MoshiBoolean -> input.value
            is MoshiDecimal -> input.value
            is MoshiLong -> input.value
            is MoshiInteger -> input.value
        }

        private fun BigInteger.canConvertToInt() =
            (this >= BigInteger.valueOf(Int.MIN_VALUE.toLong())) && (this <= BigInteger.valueOf(Int.MAX_VALUE.toLong()))

        private fun BigInteger.canConvertToLong() =
            (this >= BigInteger.valueOf(Long.MIN_VALUE)) && (this <= BigInteger.valueOf(Long.MAX_VALUE))

        private fun BigDecimal.canConvertToDouble() =
            (this >= BigDecimal.valueOf(Double.MIN_VALUE)) && (this <= BigDecimal.valueOf(Double.MAX_VALUE))
    }
}
