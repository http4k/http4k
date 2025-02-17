package org.http4k.format

import com.ubertob.kondor.json.JBigDecimal
import com.ubertob.kondor.json.JBigInteger
import com.ubertob.kondor.json.JBooleanRepresentable
import com.ubertob.kondor.json.JDouble
import com.ubertob.kondor.json.JFloat
import com.ubertob.kondor.json.JInt
import com.ubertob.kondor.json.JNumRepresentable
import com.ubertob.kondor.json.JStringRepresentable
import com.ubertob.kondor.json.JsonOutcome
import org.http4k.lens.BiDiMapping
import java.math.BigDecimal
import java.math.BigInteger

fun <OUT> BiDiMapping<String, OUT>.asJConverter() = object : JStringRepresentable<OUT>() {
    override val cons: (String) -> OUT = ::invoke
    override val render: (OUT) -> String = ::invoke
}

inline fun <reified IN : Number, OUT : Any> BiDiMapping<IN, OUT>.asJConverter() = object : JNumRepresentable<OUT>() {
    override val cons: (Number) -> OUT = { number ->
        when (IN::class) {
            Float::class -> asOut(JFloat.cons(number) as IN)
            Double::class -> asOut(JDouble.cons(number) as IN)
            Int::class -> asOut(JInt.cons(number) as IN)
            BigDecimal::class -> asOut(JBigDecimal.cons(number) as IN)
            BigInteger::class -> asOut(JBigInteger.cons(number) as IN)
            else -> throw IllegalArgumentException("Unsupported type for IN: ${IN::class}")
        }
    }

    override val render: (OUT) -> Number = { outObj ->
        when (IN::class) {
            Float::class -> JFloat.render(asIn(outObj) as Float)
            Double::class -> JDouble.render(asIn(outObj) as Double)
            Int::class -> JInt.render(asIn(outObj) as Int)
            BigDecimal::class -> JBigDecimal.render(asIn(outObj) as BigDecimal)
            BigInteger::class -> JBigInteger.render(asIn(outObj) as BigInteger)
            else -> throw IllegalArgumentException("Unsupported type for IN: ${IN::class}")
        }
    }

    override fun parser(value: String): JsonOutcome<Number> = when (IN::class) {
        Float::class -> JFloat.parser(value)
        Double::class -> JDouble.parser(value)
        Int::class -> JInt.parser(value)
        BigDecimal::class -> JBigDecimal.parser(value)
        BigInteger::class -> JBigInteger.parser(value)
        else -> throw IllegalArgumentException("Unsupported type for IN: ${IN::class}")
    }

}

fun <OUT : Any> BiDiMapping<Boolean, OUT>.asJConverter() = object : JBooleanRepresentable<OUT>() {
    override val cons: (Boolean) -> OUT = { asOut(it) }
    override val render: (OUT) -> Boolean = { asIn(it) }
}

internal fun Number.toBigDecimal() = if (this is BigDecimal) this else toString().toBigDecimal()
