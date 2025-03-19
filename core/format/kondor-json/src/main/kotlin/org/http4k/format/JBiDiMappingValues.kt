package org.http4k.format

import com.ubertob.kondor.json.JBigDecimalRepresentable
import com.ubertob.kondor.json.JBigIntegerRepresentable
import com.ubertob.kondor.json.JBooleanRepresentable
import com.ubertob.kondor.json.JDoubleRepresentable
import com.ubertob.kondor.json.JFloatRepresentable
import com.ubertob.kondor.json.JIntRepresentable
import com.ubertob.kondor.json.JLongRepresentable
import com.ubertob.kondor.json.JStringRepresentable
import com.ubertob.kondor.json.JsonConverter
import com.ubertob.kondor.json.jsonnode.JsonNodeNumber
import org.http4k.lens.BiDiMapping
import java.math.BigDecimal
import java.math.BigInteger

fun <OUT> BiDiMapping<String, OUT>.asJConverter() = object : JStringRepresentable<OUT>() {
    override val cons: (String) -> OUT = ::invoke
    override val render: (OUT) -> String = ::invoke
}

inline fun <reified IN : Number, OUT : Any> BiDiMapping<IN, OUT>.asJConverter() =
    when (IN::class) {
        Float::class -> object : JFloatRepresentable<OUT>() {
            override val cons: (Float) -> OUT = { asOut(it as IN) }
            override val render: (OUT) -> Float = { asIn(it) as Float }
        }

        Double::class -> object : JDoubleRepresentable<OUT>() {
            override val cons: (Double) -> OUT = { asOut(it as IN) }
            override val render: (OUT) -> Double = { asIn(it) as Double }

        }

        Int::class -> object : JIntRepresentable<OUT>() {
            override val cons: (Int) -> OUT = { asOut(it as IN) }
            override val render: (OUT) -> Int = { asIn(it) as Int }

        }

        Long::class -> object : JLongRepresentable<OUT>() {
            override val cons: (Long) -> OUT = { asOut(it as IN) }
            override val render: (OUT) -> Long = { asIn(it) as Long }

        }
        BigDecimal::class -> object : JBigDecimalRepresentable<OUT>() {
            override val cons: (BigDecimal) -> OUT = { asOut(it as IN) }
            override val render: (OUT) -> BigDecimal = { asIn(it) as BigDecimal }

        }

        BigInteger::class -> object : JBigIntegerRepresentable<OUT>() {
            override val cons: (BigInteger) -> OUT = { asOut(it as IN) }
            override val render: (OUT) -> BigInteger = { asIn(it) as BigInteger }

        }

        else -> {
            throw IllegalArgumentException("Unsupported type for IN: ${IN::class}")
        }
    } as JsonConverter<OUT, JsonNodeNumber>

fun <OUT : Any> BiDiMapping<Boolean, OUT>.asJConverter() = object : JBooleanRepresentable<OUT>() {
    override val cons: (Boolean) -> OUT = { asOut(it) }
    override val render: (OUT) -> Boolean = { asIn(it) }
}

internal fun Number.toBigDecimal() = this as? BigDecimal ?: toString().toBigDecimal()
