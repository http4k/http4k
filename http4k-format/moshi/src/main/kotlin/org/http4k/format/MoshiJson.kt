package org.http4k.format

import java.math.BigDecimal
import java.math.BigInteger

class MoshiJson(val moshi: com.squareup.moshi.Moshi): Json<MoshiNode> {

    private val objectAdapter = moshi.adapter(Any::class.java)

    override fun MoshiNode.asPrettyJsonString(): String = objectAdapter.indent("    ").toJson(unwrap())
    override fun MoshiNode.asCompactJsonString(): String = objectAdapter.toJson(unwrap())
    override fun String.asJsonObject(): MoshiNode {
        val map = objectAdapter.fromJson(this)
        return MoshiNode.wrap(map)
    }

    override fun <LIST : Iterable<Pair<String, MoshiNode>>> LIST.asJsonObject() = MoshiObject(toMap())
    override fun String?.asJsonValue() = if (this == null) MoshiNull else MoshiString(this)
    override fun Int?.asJsonValue() = if (this == null) MoshiNull else MoshiInteger(toLong())
    override fun Double?.asJsonValue() = if (this == null) MoshiNull else MoshiDecimal(this)
    override fun Long?.asJsonValue() = if (this == null) MoshiNull else MoshiInteger(this)
    override fun BigDecimal?.asJsonValue() = if (this == null) MoshiNull else MoshiDecimal(toDouble())
    override fun BigInteger?.asJsonValue() = if (this == null) MoshiNull else MoshiInteger(toLong())
    override fun Boolean?.asJsonValue() = if (this == null) MoshiNull else MoshiBoolean(this)
    override fun <T : Iterable<MoshiNode>> T.asJsonArray() = MoshiArray(toList())

    override fun textValueOf(node: MoshiNode, name: String): String? = (node as? MoshiObject)
        ?.attributes?.get(name)
        ?.unwrap()?.toString()

    override fun decimal(value: MoshiNode) = (value as MoshiDecimal).value.toBigDecimal()
    override fun integer(value: MoshiNode) = ((value as MoshiInteger).value)
    override fun bool(value: MoshiNode) = (value as MoshiBoolean).value
    override fun text(value: MoshiNode) = (value as MoshiString).value
    override fun elements(value: MoshiNode) = (value as MoshiArray).elements
    override fun fields(node: MoshiNode) = (node as? MoshiObject)
        ?.attributes
        ?.map { it.key to it.value }
        ?: emptyList()

    override fun typeOf(value: MoshiNode) = when(value) {
        is MoshiNull -> JsonType.Null
        is MoshiObject -> JsonType.Object
        is MoshiArray -> JsonType.Array
        is MoshiInteger -> JsonType.Integer
        is MoshiDecimal -> JsonType.Number
        is MoshiString -> JsonType.String
        is MoshiBoolean -> JsonType.Boolean
    }
}
