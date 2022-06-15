package org.http4k.format

import java.math.BigDecimal
import java.math.BigInteger

object MoshiJson: Json<MoshiElement> {

    override fun MoshiElement.asPrettyJsonString() = toJson { writer -> writer.indent = "  " }
    override fun MoshiElement.asCompactJsonString() = toJson()
    override fun String.asJsonObject() = MoshiElement.parse(this)

    override fun <LIST : Iterable<Pair<String, MoshiElement>>> LIST.asJsonObject() = MoshiObject(toMap())
    override fun String?.asJsonValue() = if (this == null) MoshiNull else MoshiString(this)
    override fun Int?.asJsonValue() = if (this == null) MoshiNull else MoshiNumber(this)
    override fun Double?.asJsonValue() = if (this == null) MoshiNull else MoshiNumber(this)
    override fun Long?.asJsonValue() = if (this == null) MoshiNull else MoshiNumber(this)
    override fun BigDecimal?.asJsonValue() = if (this == null) MoshiNull else MoshiNumber(this)
    override fun BigInteger?.asJsonValue() = if (this == null) MoshiNull else MoshiNumber(this)
    override fun Boolean?.asJsonValue() = if (this == null) MoshiNull else MoshiBoolean(this)
    override fun <T : Iterable<MoshiElement>> T.asJsonArray() = MoshiArray(toList())

    override fun textValueOf(node: MoshiElement, name: String) = (node as MoshiObject)
        .attributes[name]
        ?.let { text(it) }

    override fun decimal(value: MoshiElement) = when(val num = (value as MoshiNumber).value) {
        is Long -> num.toBigDecimal()
        is Int -> num.toBigDecimal()
        is Float -> num.toBigDecimal()
        is Double -> num.toBigDecimal()
        is BigDecimal -> num
        is BigInteger -> num.toBigDecimal()
        else -> throw java.lang.IllegalArgumentException("Cannot convert $value to BigDecimal")
    }

    override fun integer(value: MoshiElement) = ((value as MoshiNumber).value).toLong()
    override fun bool(value: MoshiElement) = (value as MoshiBoolean).value
    override fun text(value: MoshiElement) = (value as MoshiString).value
    override fun elements(value: MoshiElement) = (value as MoshiArray).elements
    override fun fields(node: MoshiElement) = (node as MoshiObject).attributes.map { it.key to it.value }

    override fun typeOf(value: MoshiElement) = when(value) {
        is MoshiObject -> JsonType.Object
        is MoshiArray -> JsonType.Array
        is MoshiNull -> JsonType.Null
        is MoshiNumber -> JsonType.Number
        is MoshiString -> JsonType.String
        is MoshiBoolean -> JsonType.Boolean
    }
}
