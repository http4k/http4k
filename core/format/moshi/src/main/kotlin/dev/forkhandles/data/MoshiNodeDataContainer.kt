package dev.forkhandles.data

import org.http4k.format.MoshiArray
import org.http4k.format.MoshiBoolean
import org.http4k.format.MoshiDecimal
import org.http4k.format.MoshiInteger
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiNull
import org.http4k.format.MoshiObject
import org.http4k.format.MoshiString
import java.math.BigDecimal
import java.math.BigInteger
import kotlin.Int.Companion.MIN_VALUE

/**
 * Moshi MoshiNode-based implementation of the DataContainer
 */
open class MoshiNodeDataContainer(input: MoshiNode) :
    DataContainer<MoshiNode>(
        input,
        { content, it -> (content as? MoshiObject)?.attributes?.containsKey(it) ?: false },
        { content, it ->
            println("content: $content, it: $it")
            (content as? MoshiObject)?.attributes?.get(it)?.let { nodeToValue(it) }
        },
        { node, name, value ->
            println("node: $node, name: $name, value: $value")
            (node as? MoshiObject)?.also { it.attributes[name] = value.toNode() }
                ?.also { println("it: $it") }
                ?: error("Invalid node type ${input::class.java}")
        }
    ) {

    companion object {
        private fun Any?.toNode(): MoshiNode = when (this) {
            null -> MoshiNull
            is MoshiNode -> this
            is DataContainer<*> -> unwrap().toNode()
            is Boolean -> MoshiBoolean(this)
            is Short -> MoshiInteger(toLong())
            is Int -> MoshiInteger(toLong())
            is Long -> MoshiInteger(this)
            is BigInteger -> when (canConvertToLong()) {
                true -> MoshiInteger(toLong())
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
            is MoshiInteger -> {
                println(input)
                when {
                    input.canConvertToInt() -> input.value.toInt()
                    else -> input.value
                }
            }
        }

        private fun BigInteger.canConvertToLong() =
            (this >= BigInteger.valueOf(Long.MIN_VALUE)) && (this <= BigInteger.valueOf(Long.MAX_VALUE))

        private fun BigDecimal.canConvertToDouble() =
            (this >= BigDecimal.valueOf(Double.MIN_VALUE)) && (this <= BigDecimal.valueOf(Double.MAX_VALUE))

        private fun MoshiInteger.canConvertToInt() =
            (value >= MIN_VALUE.toLong()) && (value <= Int.MAX_VALUE.toLong())
    }
}
