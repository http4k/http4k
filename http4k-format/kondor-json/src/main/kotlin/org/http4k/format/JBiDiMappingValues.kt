package org.http4k.format

import com.ubertob.kondor.json.CharWriter
import com.ubertob.kondor.json.JNumRepresentable
import com.ubertob.kondor.json.JStringRepresentable
import com.ubertob.kondor.json.JsonConverter
import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.json.JsonStyle
import com.ubertob.kondor.json.JsonStyle.Companion.appendBoolean
import com.ubertob.kondor.json.jsonnode.BooleanNode
import com.ubertob.kondor.json.jsonnode.JsonNodeBoolean
import com.ubertob.kondor.json.jsonnode.NodeKind
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.tryFromNode
import org.http4k.lens.BiDiMapping
import java.math.BigDecimal

fun <OUT> BiDiMapping<String, OUT>.asJConverter() = object : JStringRepresentable<OUT>() {
    override val cons: (String) -> OUT = ::invoke
    override val render: (OUT) -> String = ::invoke
}

fun <IN : Number, OUT : Any> BiDiMapping<IN, OUT>.asJConverter(valueConverter: JNumRepresentable<IN>) = object : JNumRepresentable<OUT>() {
    override val cons: (Number) -> OUT = { asOut(valueConverter.cons(it)) }
    override val render: (OUT) -> BigDecimal = { valueConverter.render(asIn(it)).toBigDecimal() }
}

fun <OUT> BiDiMapping<Boolean, OUT>.asJConverter() = object : JsonConverter<OUT, JsonNodeBoolean> {
    override val _nodeType: NodeKind<JsonNodeBoolean> = BooleanNode
    override fun fromJsonNode(node: JsonNodeBoolean, path: NodePath): JsonOutcome<OUT>  = tryFromNode(path) { invoke(node.boolean) }
    override fun toJsonNode(value: OUT): JsonNodeBoolean = JsonNodeBoolean(invoke(value))
    override fun appendValue(app: CharWriter, style: JsonStyle, offset: Int, value: OUT): CharWriter = app.appendBoolean(asIn(value))
}

internal fun Number.toBigDecimal() = if (this is BigDecimal) this else toString().toBigDecimal()
