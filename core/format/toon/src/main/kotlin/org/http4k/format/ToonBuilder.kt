package org.http4k.format

import com.squareup.moshi.Moshi
import org.http4k.lens.BiDiMapping
import java.math.BigDecimal
import java.math.BigInteger

class ToonBuilder(private val moshiBuilder: AutoMappingConfiguration<Moshi.Builder> = standardConfig()) {

    fun build() = ConfigurableMoshi(moshiBuilder.done())

    fun asConfigurable() = object : AutoMappingConfiguration<ToonBuilder> {
        override fun <OUT> boolean(mapping: BiDiMapping<Boolean, OUT>) = apply { moshiBuilder.boolean(mapping) }

        override fun <OUT> int(mapping: BiDiMapping<Int, OUT>) = apply { moshiBuilder.int(mapping) }

        override fun <OUT> long(mapping: BiDiMapping<Long, OUT>) = apply { moshiBuilder.long(mapping) }

        override fun <OUT> double(mapping: BiDiMapping<Double, OUT>) = apply { moshiBuilder.double(mapping) }

        override fun <OUT> bigInteger(mapping: BiDiMapping<BigInteger, OUT>) = apply { moshiBuilder.bigInteger(mapping) }

        override fun <OUT> bigDecimal(mapping: BiDiMapping<BigDecimal, OUT>) = apply { moshiBuilder.bigDecimal(mapping) }

        override fun <OUT> text(mapping: BiDiMapping<String, OUT>) = apply { moshiBuilder.text(mapping) }

        override fun done() = this@ToonBuilder
    }
}
