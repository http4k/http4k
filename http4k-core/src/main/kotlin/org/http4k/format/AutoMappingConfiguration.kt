package org.http4k.format

import org.http4k.lens.BiDiMapping
import org.http4k.lens.StringBiDiMappings
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
    text(StringBiDiMappings.duration())
    text(StringBiDiMappings.uri())
    text(StringBiDiMappings.url())
    text(StringBiDiMappings.uuid())
    text(StringBiDiMappings.regexObject())
    text(StringBiDiMappings.instant())
    text(StringBiDiMappings.localTime())
    text(StringBiDiMappings.localDate())
    text(StringBiDiMappings.localDateTime())
    text(StringBiDiMappings.zonedDateTime())
    text(StringBiDiMappings.offsetTime())
    text(StringBiDiMappings.offsetDateTime())
}