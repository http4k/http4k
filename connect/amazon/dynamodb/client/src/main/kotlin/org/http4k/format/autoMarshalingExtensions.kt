package org.http4k.format

import org.http4k.connect.amazon.dynamodb.model.AttributeName
import org.http4k.connect.amazon.dynamodb.model.AttributeValue
import org.http4k.connect.amazon.dynamodb.model.Item
import org.http4k.lens.BiDiLens
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta.ObjectParam

/**
 * Auto object mapping for items going into and out of Dynamo tables. The fields are flattened
 * across the table, and complex objects and lists are preserved in place.
 */
inline fun <reified T : Any> AutoMarshalling.autoDynamoLens() = BiDiLens<Item, T>(
    Meta(true, "dynamo", ObjectParam, "item", null, emptyMap()),
    {
        convert(it.map { it.key.value to fromAttributeValue(it.value) }.toMap())
    },
    { t, item ->
        convert<T, Map<String, Any?>>(t).map(::toAttributeMapping)
            .fold(item) { acc, next -> next(acc) }
    }
)

fun toAttributeMapping(it: Map.Entry<String, Any?>): (Item) -> Item = { item: Item ->
    item + (AttributeName.of(it.key) to toAttributeValue(it.value))
}

fun fromAttributeValue(value: AttributeValue): Any? = value.N
    ?: value.BOOL
    ?: value.S
    ?: value.M?.map { it.key.value to fromAttributeValue(it.value) }?.toMap()
    ?: value.L?.map(::fromAttributeValue)?.toTypedArray()
    ?: value.SS?.toTypedArray()
    ?: value.NS?.toTypedArray()

@Suppress("UNCHECKED_CAST")
private fun toAttributeValue(value: Any?): AttributeValue = when (value) {
    null -> AttributeValue.Null()
    is Number -> AttributeValue.Num(value)
    is Boolean -> AttributeValue.Bool(value)
    is Map<*, *> -> (value as Map<String, Any?>).let {
        AttributeValue.Map(Item(*it.map(::toAttributeMapping).toTypedArray()))
    }

    is Iterable<*> -> AttributeValue.List(value.map(::toAttributeValue))
    else -> AttributeValue.Str(value.toString())
}
