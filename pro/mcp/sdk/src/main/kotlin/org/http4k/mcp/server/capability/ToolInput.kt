package org.http4k.mcp.server.capability

import dev.forkhandles.data.DataProperty
import dev.forkhandles.data.Metadatum
import dev.forkhandles.data.nodeToValue
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import dev.forkhandles.values.Value
import dev.forkhandles.values.ValueFactory
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible

@Suppress("UNCHECKED_CAST")
abstract class ToolInput(private val input: MoshiNode) {

    private val existsFn =
        { content: MoshiNode, it: String -> (content as? MoshiObject)?.attributes?.containsKey(it) ?: false }

    private val getFn = { content: MoshiNode, it: String ->
        (content as? MoshiObject)?.attributes?.get(it)?.let { nodeToValue(it) }
    }

    protected fun <OUT : Any> required(description: String) =
        property({ it: OUT -> it }, description)

    protected fun <OUT> optional(description: String) = property<OUT?, OUT>({ it }, description)

    protected fun <IN : Any, OUT : Value<IN>> required(factory: ValueFactory<OUT, IN>, description: String) =
        property(factory.parse(), description)


    protected fun <IN : Any, OUT : Value<IN>> optional(factory: ValueFactory<OUT, IN>, description: String) =
        property<OUT?, IN>(factory.parse(), description)

    protected fun <OUT : ToolInput> requiredObj(mapInFn: (MoshiNode) -> OUT, description: String) =
        property(mapInFn, description)

    protected fun <OUT : ToolInput> optionalObj(mapInFn: (MoshiNode) -> OUT, description: String) =
        property<OUT?, MoshiNode>(mapInFn, description)

    protected fun <OUT> requiredList(description: String) = requiredList<OUT, OUT>({ it }, description)

    protected fun <IN : Any, OUT : Value<IN>> requiredList(factory: ValueFactory<OUT, IN>, description: String) =
        requiredList(factory.parse(), description)

    protected fun <OUT> optionalList(description: String) = optionalList<OUT, OUT & Any>({ it }, description)

    protected fun <IN : Any, OUT : Value<IN>> optionalList(factory: ValueFactory<OUT, IN>, description: String) =
        optionalList(factory.parse(), description)

    internal fun propertyMetadata(): List<PropertyMetadata> = kClass().memberProperties
        .mapNotNull { prop ->
            prop.isAccessible = true
            val delegate = prop.getDelegate(this)
            when {
                delegate is DataProperty<*, *> -> PropertyMetadata(prop.name, prop.returnType, delegate.data)
                else -> null
            }
        }

    fun unwrap() = input

    private fun <OUT, IN> requiredList(mapInFn: (IN) -> OUT, description: String) =
        property<List<OUT>, List<IN>>({ it.map(mapInFn) }, description)

    private fun <OUT, IN> optionalList(mapInFn: (IN) -> OUT, description: String) =
        property<List<OUT>?, List<IN>>({ it.map(mapInFn) }, description)

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ToolInput

        return unwrap() == other.unwrap()
    }

    override fun hashCode() = unwrap().hashCode()

    override fun toString() = unwrap().toString()

    private fun <IN, OUT : Any?> property(
        mapInFn: (OUT) -> IN,
        description: String
    ) = DataProperty<ToolInput, IN>(
        { name -> existsFn(unwrap(), name) },
        { name -> getFn(unwrap(), name)?.let { value -> value as OUT }?.let(mapInFn) },
        { _, _ -> error("unsupported") },
        listOf(Description.of(description)),
    )

    private fun Any.kClass() = this::class as KClass<ToolInput>

    private fun <IN : Any, OUT : Value<IN>> ValueFactory<OUT, IN>.parse(): (IN) -> OUT = {
        when (it) {
            is String, is Boolean, is Number -> parse(it.toString())
            else -> of(it)
        }
    }
}

data class PropertyMetadata(val name: String, val type: KType, val data: List<Metadatum>)

class Description private constructor(value: String) : StringValue(value), Metadatum {
    companion object : NonBlankStringValueFactory<Description>(::Description)
}
