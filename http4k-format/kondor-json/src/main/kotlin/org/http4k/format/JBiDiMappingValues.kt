package org.http4k.format

import com.ubertob.kondor.json.JStringRepresentable
import com.ubertob.kondor.json.JsonConverter
import com.ubertob.kondor.json.JsonOutcome
import com.ubertob.kondor.json.jsonnode.BooleanNode
import com.ubertob.kondor.json.jsonnode.JsonNodeBoolean
import com.ubertob.kondor.json.jsonnode.JsonNodeNumber
import com.ubertob.kondor.json.jsonnode.NodeKind
import com.ubertob.kondor.json.jsonnode.NodePath
import com.ubertob.kondor.json.jsonnode.NumberNode
import com.ubertob.kondor.json.tryFromNode
import org.http4k.lens.BiDiMapping
import java.math.BigDecimal
import java.math.BigInteger

class JBiDiMappingBoolean<OUT>(private val mapping: BiDiMapping<Boolean, OUT>) : JsonConverter<OUT, JsonNodeBoolean> {
    override val _nodeType: NodeKind<JsonNodeBoolean> = BooleanNode
    override fun fromJsonNode(node: JsonNodeBoolean): JsonOutcome<OUT> = tryFromNode(node) { mapping(node.value) }
    override fun toJsonNode(value: OUT, path: NodePath): JsonNodeBoolean = JsonNodeBoolean(mapping(value), path)
}

class JBiDiMappingInt<OUT>(private val mapping: BiDiMapping<Int, OUT>) : JsonConverter<OUT, JsonNodeNumber> {
    override val _nodeType: NodeKind<JsonNodeNumber> = NumberNode
    override fun fromJsonNode(node: JsonNodeNumber): JsonOutcome<OUT> = tryFromNode(node) { mapping(node.num.intValueExact()) }
    override fun toJsonNode(value: OUT, path: NodePath): JsonNodeNumber = JsonNodeNumber(mapping(value).toBigDecimal(), path)
}

class JBiDiMappingLong<OUT>(private val mapping: BiDiMapping<Long, OUT>) : JsonConverter<OUT, JsonNodeNumber> {
    override val _nodeType: NodeKind<JsonNodeNumber> = NumberNode
    override fun fromJsonNode(node: JsonNodeNumber): JsonOutcome<OUT> = tryFromNode(node) { mapping(node.num.longValueExact()) }
    override fun toJsonNode(value: OUT, path: NodePath): JsonNodeNumber = JsonNodeNumber(mapping(value).toBigDecimal(), path)
}

class JBiDiMappingDouble<OUT>(private val mapping: BiDiMapping<Double, OUT>) : JsonConverter<OUT, JsonNodeNumber> {
    override val _nodeType: NodeKind<JsonNodeNumber> = NumberNode
    override fun fromJsonNode(node: JsonNodeNumber): JsonOutcome<OUT> = tryFromNode(node) { mapping(node.num.toDouble()) }
    override fun toJsonNode(value: OUT, path: NodePath): JsonNodeNumber = JsonNodeNumber(mapping(value).toBigDecimal(), path)
}

class JBiDiMappingBigInteger<OUT>(private val mapping: BiDiMapping<BigInteger, OUT>) : JsonConverter<OUT, JsonNodeNumber> {
    override val _nodeType: NodeKind<JsonNodeNumber> = NumberNode
    override fun fromJsonNode(node: JsonNodeNumber): JsonOutcome<OUT> = tryFromNode(node) { mapping(node.num.toBigIntegerExact()) }
    override fun toJsonNode(value: OUT, path: NodePath): JsonNodeNumber = JsonNodeNumber(mapping(value).toBigDecimal(), path)
}

class JBiDiMappingBigDecimal<OUT>(private val mapping: BiDiMapping<BigDecimal, OUT>): JsonConverter<OUT, JsonNodeNumber> {
    override val _nodeType: NodeKind<JsonNodeNumber> = NumberNode
    override fun fromJsonNode(node: JsonNodeNumber): JsonOutcome<OUT> = tryFromNode(node) { mapping(node.num) }
    override fun toJsonNode(value: OUT, path: NodePath): JsonNodeNumber = JsonNodeNumber(mapping(value), path)
}

class JBiDiMappingString<OUT>(private val mapping: BiDiMapping<String, OUT>) : JStringRepresentable<OUT>() {
    override val cons: (String) -> OUT = mapping::invoke
    override val render: (OUT) -> String = mapping::invoke
}
