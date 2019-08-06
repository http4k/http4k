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
interface AutoMappingConfiguration<BUILDER> {
    fun <OUT> boolean(mapping: BiDiMapping<Boolean, OUT>): AutoMappingConfiguration<BUILDER>
    fun <OUT> int(mapping: BiDiMapping<Int, OUT>): AutoMappingConfiguration<BUILDER>
    fun <OUT> long(mapping: BiDiMapping<Long, OUT>): AutoMappingConfiguration<BUILDER>
    fun <OUT> double(mapping: BiDiMapping<Double, OUT>): AutoMappingConfiguration<BUILDER>
    fun <OUT> bigInteger(mapping: BiDiMapping<BigInteger, OUT>): AutoMappingConfiguration<BUILDER>
    fun <OUT> bigDecimal(mapping: BiDiMapping<BigDecimal, OUT>): AutoMappingConfiguration<BUILDER>
    fun <OUT> text(mapping: BiDiMapping<String, OUT>): AutoMappingConfiguration<BUILDER>

    /**
     * Prevent the unmarshalling of raw (unbounded) strings. Useful when we are taking data from the Internet and want
     * to ensure that all inbound fields are represented by bounded or validated types.
     */
    fun prohibitStrings(): AutoMappingConfiguration<BUILDER> = text(BiDiMapping<String, String>({
        throw IllegalArgumentException("Unmarshalling unbounded strings is prohibited")
    }, { it }))

    /**
     * Finalise the mapping configurations.
     */
    fun done(): BUILDER
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

/**
 * This is the set of utility methods which avoid the creation of a BiDiMapping.
 */
inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.text(noinline inMapping: (String) -> OUT,
                                                                         noinline outMapping: (OUT) -> String) = text(BiDiMapping(inMapping, outMapping))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.boolean(noinline inMapping: (Boolean) -> OUT,
                                                                            noinline outMapping: (OUT) -> Boolean) = boolean(BiDiMapping(inMapping, outMapping))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.int(noinline inMapping: (Int) -> OUT,
                                                                        noinline outMapping: (OUT) -> Int) = int(BiDiMapping(inMapping, outMapping))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.long(noinline inMapping: (Long) -> OUT,
                                                                         noinline outMapping: (OUT) -> Long) = long(BiDiMapping(inMapping, outMapping))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.double(noinline outMapping: (OUT) -> Double,
                                                                           noinline inMapping: (Double) -> OUT) = double(BiDiMapping(inMapping, outMapping))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.bigInteger(noinline inMapping: (BigInteger) -> OUT,
                                                                               noinline outMapping: (OUT) -> BigInteger) = bigInteger(BiDiMapping(inMapping, outMapping))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.bigDecimal(noinline inMapping: (BigDecimal) -> OUT,
                                                                               noinline outMapping: (OUT) -> BigDecimal) = bigDecimal(BiDiMapping(inMapping, outMapping))

/**
 * Utility method for when only writing/serialization is required
 */
@JvmName("textSerialize")
inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.text(noinline mapping: (OUT) -> String): AutoMappingConfiguration<BUILDER> = text<OUT>(BiDiMapping(OUT::class.java, { throw java.lang.IllegalArgumentException() }, mapping))

/**
 * Utility method for when only reading/deserialization is required
 */
@JvmName("textDeserialize")
inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.text(noinline mapping: (String) -> OUT): AutoMappingConfiguration<BUILDER> = text<OUT>(BiDiMapping(OUT::class.java, mapping, { throw java.lang.IllegalArgumentException() }))
