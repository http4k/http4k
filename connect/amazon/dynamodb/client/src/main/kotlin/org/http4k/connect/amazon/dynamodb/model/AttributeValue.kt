package org.http4k.connect.amazon.dynamodb.model

import org.http4k.connect.model.Base64Blob
import se.ansman.kotshi.JsonSerializable
import java.math.BigDecimal

/**
 * Represents the on-the-wire format of an Attribute Value with it's requisite type.
 * Only one of these fields is ever populated at once in an entry. So you can get
 * { "S": "hello" } or { "BOOL": true } or { "NS": ["123"] }
 */
@JsonSerializable
@ExposedCopyVisibility
data class AttributeValue internal constructor(
    val B: Base64Blob? = null,
    val BOOL: Boolean? = null,
    val BS: Set<Base64Blob>? = null,
    val L: List<AttributeValue>? = null,
    val M: Item? = null,
    val N: String? = null,
    val NS: Set<String>? = null,
    val NULL: Boolean? = null,
    val S: String? = null,
    val SS: Set<String>? = null
) : Comparable<AttributeValue> {

    override fun toString() = "AttributeValue(" +
        when {
            B != null -> "B=$B"
            BOOL != null -> "BOOL=$BOOL"
            BS != null -> "BS=$BS"
            L != null -> "L=$L"
            M != null -> "M=$M"
            N != null -> "N=$N"
            NS != null -> "NS=$NS"
            NULL != null -> "NULL=$NULL"
            S != null -> "S=$S"
            SS != null -> "SS=$SS"
            else -> error("illegal!")
        } +
        ")"

    override fun hashCode() = when {
        B != null -> B.hashCode()
        BOOL != null -> "bool$BOOL".hashCode()
        BS != null -> BS.hashCode()
        L != null -> L.hashCode()
        M != null -> M.hashCode()
        N != null -> N.hashCode()
        NS != null -> NS.hashCode()
        NULL != null -> "null$NULL".hashCode()
        S != null -> S.hashCode()
        SS != null -> SS.hashCode()
        else -> 0
    }

    override fun equals(other: Any?): Boolean {
        return when {
            other !is AttributeValue -> false
            B != null && other.B != null -> B == other.B
            BOOL != null -> BOOL == other.BOOL
            BS != null -> BS == other.BS
            L != null -> L == other.L
            M != null -> M == other.M
            // N must be equated by comparing as BigDecimal; otherwise 123 != 123.0
            N != null && other.N != null -> N.toBigDecimal().compareTo(other.N.toBigDecimal()) == 0
            NS != null && other.NS != null -> {
                if (NS.size != other.NS.size) return false
                val thisNumbers = NS.map { it.toBigDecimal() }.sorted()
                val otherNumbers = other.NS.map { it.toBigDecimal() }.sorted()
                thisNumbers.zip(otherNumbers).all { it.first.compareTo(it.second) == 0 }
            }

            NULL != null -> NULL == other.NULL
            S != null -> S == other.S
            SS != null -> SS == other.SS
            else -> false
        }
    }

    override fun compareTo(other: AttributeValue) = when {
        S != null && other.S != null -> S.compareTo(other.S)
        N != null && other.N != null -> N.toBigDecimal().compareTo(other.N.toBigDecimal())
        B != null && other.B != null -> B.decoded().compareTo(other.B.decoded())
        else -> 0
    }

    operator fun plus(other: AttributeValue) = when {
        N != null && other.N != null -> Num(N.toBigDecimal() + other.N.toBigDecimal())
        NS != null && other.NS != null -> NumSet((NS + other.NS).map { it.toBigDecimal() }.toSet())
        SS != null && other.SS != null -> StrSet((SS + other.SS))
        BS != null && other.BS != null -> Base64Set(BS + other.BS)
        L != null && other.L != null -> List(L + other.L)
        else -> this
    }

    operator fun minus(other: AttributeValue) = when {
        N != null && other.N != null -> Num(N.toBigDecimal() - other.N.toBigDecimal())
        SS != null && other.SS != null -> StrSet((SS - other.SS))
        NS != null && other.NS != null -> NumSet((NS - other.NS).map { it.toBigDecimal() }.toSet())
        BS != null && other.BS != null -> Base64Set(BS - other.BS)
        else -> this
    }

    fun with(index: Int, value: AttributeValue) = when {
        L != null -> if (index > L.lastIndex) {
            List(L + value)
        } else {
            List(L.toMutableList().apply { set(index.coerceAtMost(L.lastIndex), value) }.toList()) // FIXME do immutably
        }

        else -> this
    }

    fun delete(index: Int) = when {
        L != null -> List(
            L.subList(0, index.coerceAtMost(L.size)) + L.subList(
                (index + 1).coerceAtMost(L.size),
                L.size
            )
        )

        else -> this
    }

    companion object {
        fun Base64(value: Base64Blob?) = value?.let { AttributeValue(B = it) } ?: Null()
        fun Bool(value: Boolean?) = value?.let { AttributeValue(BOOL = it) } ?: Null()
        fun Base64Set(value: Set<Base64Blob>?) = value?.let { AttributeValue(BS = it) } ?: Null()
        fun List(value: List<AttributeValue>?) = value?.let { AttributeValue(L = it) } ?: Null()
        fun Map(value: Item?) = value?.let { AttributeValue(M = it) } ?: Null()
        fun Num(value: Number?) = value?.let { AttributeValue(N = it.toString()) } ?: Null()
        fun NumSet(value: Set<Number>?) = value?.let { AttributeValue(NS = it.map { it.toString() }.toSet()) } ?: Null()
        fun Null() = AttributeValue(NULL = true)
        fun Str(value: String?) = value?.let { AttributeValue(S = it) } ?: Null()
        fun StrSet(value: Set<String>?) = value?.let { AttributeValue(SS = it.map { it }.toSet()) } ?: Null()

        @Suppress("UNCHECKED_CAST")
        fun from(key: DynamoDataType, value: Any): AttributeValue = when (key) {
            DynamoDataType.B -> Base64(Base64Blob.of(value as String))
            DynamoDataType.BOOL -> Bool(value.toString().toBoolean())
            DynamoDataType.BS -> Base64Set((value as List<String>).map(Base64Blob::of).toSet())
            DynamoDataType.L -> List((value as List<Map<String, Any>>).map { it.toAttributeValue() })
            DynamoDataType.M -> Map(
                (value as Map<String, Map<String, Any>>)
                    .map { AttributeName.of(it.key) to it.value.toAttributeValue() }.toMap()
            )

            DynamoDataType.N -> Num(BigDecimal(value as String))
            DynamoDataType.NS -> NumSet((value as List<String>).map(::BigDecimal).toSet())
            DynamoDataType.NULL -> Null()
            DynamoDataType.S -> Str(value as String)
            DynamoDataType.SS -> StrSet((value as List<String>).toSet())
        }

        private fun Map<String, Any>.toAttributeValue(): AttributeValue =
            entries.first().let { (k, v) -> from(DynamoDataType.valueOf(k), v) }
    }
}
