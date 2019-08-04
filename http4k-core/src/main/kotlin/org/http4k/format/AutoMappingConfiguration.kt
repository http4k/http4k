package org.http4k.format

import org.http4k.lens.BiDiMapping
import org.http4k.lens.StringBiDiMappings.duration
import org.http4k.lens.StringBiDiMappings.instant
import org.http4k.lens.StringBiDiMappings.localDate
import org.http4k.lens.StringBiDiMappings.localDateTime
import org.http4k.lens.StringBiDiMappings.localTime
import org.http4k.lens.StringBiDiMappings.offsetDateTime
import org.http4k.lens.StringBiDiMappings.offsetTime
import org.http4k.lens.StringBiDiMappings.regexObject
import org.http4k.lens.StringBiDiMappings.uri
import org.http4k.lens.StringBiDiMappings.url
import org.http4k.lens.StringBiDiMappings.uuid
import org.http4k.lens.StringBiDiMappings.zonedDateTime
import java.math.BigDecimal
import java.math.BigInteger

/**
 * This is the generic interface used to configure auto-mapping functionality for message format libraries.
 * The various methods here can be used to provide custom mapping behaviour (say for domain classes).
 */
interface AutoMappingConfiguration<T> {
    fun <OUT> boolean(mapping: BiDiMapping<Boolean, OUT>): AutoMappingConfiguration<T>
    fun <OUT> int(mapping: BiDiMapping<Int, OUT>): AutoMappingConfiguration<T>
    fun <OUT> long(mapping: BiDiMapping<Long, OUT>): AutoMappingConfiguration<T>
    fun <OUT> double(mapping: BiDiMapping<Double, OUT>): AutoMappingConfiguration<T>
    fun <OUT> bigInteger(mapping: BiDiMapping<BigInteger, OUT>): AutoMappingConfiguration<T>
    fun <OUT> bigDecimal(mapping: BiDiMapping<BigDecimal, OUT>): AutoMappingConfiguration<T>
    fun <OUT> text(mapping: BiDiMapping<String, OUT>): AutoMappingConfiguration<T>

    /**
     * Prevent the unmarshalling of raw (unbounded) strings. Useful when we are taking data from the Internet and want
     * to ensure that all inbound fields are represented by bounded or validated types.
     */
    fun prohibitStrings(): AutoMappingConfiguration<T> = text(BiDiMapping<String, String>({
        throw IllegalArgumentException("Unmarshalling unbounded strings is prohibited")
    }, { it }))

    /**
     * Finalise the mapping configurations.
     */
    fun done(): T
}

/**
 * This is the set of (additional) standardised string <-> type mappings which http4k supports out of the box.
 */
fun <T> AutoMappingConfiguration<T>.withStandardMappings() = apply {
    text(duration())
    text(uri())
    text(url())
    text(uuid())
    text(regexObject())
    text(instant())
    text(localTime())
    text(localDate())
    text(localDateTime())
    text(zonedDateTime())
    text(offsetTime())
    text(offsetDateTime())
}