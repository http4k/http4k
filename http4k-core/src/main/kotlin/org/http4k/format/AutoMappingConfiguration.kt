package org.http4k.format

import org.http4k.lens.BiDiMapping
import org.http4k.lens.duration
import org.http4k.lens.instant
import org.http4k.lens.localDate
import org.http4k.lens.localDateTime
import org.http4k.lens.localTime
import org.http4k.lens.offsetDateTime
import org.http4k.lens.offsetTime
import org.http4k.lens.regexObject
import org.http4k.lens.uri
import org.http4k.lens.url
import org.http4k.lens.uuid
import org.http4k.lens.zonedDateTime
import java.math.BigDecimal
import java.math.BigInteger

/**
 * This is the main interface which should be
 */
interface AutoMappingConfiguration<T> {
    /**
     * Add a customised boolean <-> T mapping to this JSON instance.
     */
    fun <OUT> boolean(mapping: BiDiMapping<Boolean, OUT>): AutoMappingConfiguration<T>

    /**
     * Add a customised number <-> T mapping to this JSON instance.
     */
    fun <OUT> number(mapping: BiDiMapping<BigInteger, OUT>): AutoMappingConfiguration<T>

    /**
     * Add a customised decimal <-> T mapping to this JSON instance.
     */
    fun <OUT> decimal(mapping: BiDiMapping<BigDecimal, OUT>): AutoMappingConfiguration<T>

    /**
     * Add a customised string <-> T mapping to this JSON instance.
     */
    fun <OUT> text(mapping: BiDiMapping<String, OUT>): AutoMappingConfiguration<T>

    /**
     * Finalise the mapping configurations.
     */
    fun done(): T
}

/**
 * This is the set of (additional) standardised string <-> type mappings which http4k supports out of the box.
 */
fun <T> AutoMappingConfiguration<T>.withStandardMappings() = apply {
    text(BiDiMapping.duration())
    text(BiDiMapping.uri())
    text(BiDiMapping.url())
    text(BiDiMapping.uuid())
    text(BiDiMapping.regexObject())
    text(BiDiMapping.instant())
    text(BiDiMapping.localTime())
    text(BiDiMapping.localDate())
    text(BiDiMapping.localDateTime())
    text(BiDiMapping.zonedDateTime())
    text(BiDiMapping.offsetTime())
    text(BiDiMapping.offsetDateTime())
}