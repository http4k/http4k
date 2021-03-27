package org.http4k.format

import com.beust.klaxon.Converter
import com.beust.klaxon.JsonValue
import org.http4k.lens.BiDiMapping
import java.math.BigDecimal
import java.math.BigInteger
import com.beust.klaxon.Klaxon as KKlaxon

fun KKlaxon.asConfigurable(klaxon: KKlaxon): AutoMappingConfiguration<KKlaxon> = object : AutoMappingConfiguration<KKlaxon> {

    override fun <OUT> boolean(mapping: BiDiMapping<Boolean, OUT>) = apply {
        addConverter(mapping)
    }

    override fun <OUT> int(mapping: BiDiMapping<Int, OUT>) = apply {
        addConverter(mapping)
    }

    override fun <OUT> long(mapping: BiDiMapping<Long, OUT>) = apply {
        addConverter(mapping)
    }

    override fun <OUT> double(mapping: BiDiMapping<Double, OUT>) = apply {
        addConverter(mapping)
    }

    override fun <OUT> bigInteger(mapping: BiDiMapping<BigInteger, OUT>) = apply {
        addConverter(mapping)
    }

    override fun <OUT> bigDecimal(mapping: BiDiMapping<BigDecimal, OUT>) = apply {
        addConverter(mapping)
    }

    override fun <OUT> text(mapping: BiDiMapping<String, OUT>) = apply {
        addConverter(mapping)
    }

    private fun <IN, OUT> addConverter(mapping: BiDiMapping<IN, OUT>,) {
        klaxon.converter(object : Converter {
            override fun canConvert(cls: Class<*>) = cls == mapping.clazz

            @Suppress("UNCHECKED_CAST")
            override fun fromJson(jv: JsonValue) = mapping.asOut(jv.inside as IN)

            @Suppress("UNCHECKED_CAST")
            override fun toJson(value: Any): String {
                val asIn = mapping.asIn(value as OUT)
                return if (asIn is String) """"$asIn"""" else asIn.toString()
            }
        })
    }

    override fun done(): KKlaxon = klaxon
}
