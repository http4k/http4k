package org.http4k.format

import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.lens.BiDiMapping
import org.http4k.lens.StringBiDiMappings.duration
import org.http4k.lens.StringBiDiMappings.eventCategory
import org.http4k.lens.StringBiDiMappings.instant
import org.http4k.lens.StringBiDiMappings.localDate
import org.http4k.lens.StringBiDiMappings.localDateTime
import org.http4k.lens.StringBiDiMappings.localTime
import org.http4k.lens.StringBiDiMappings.offsetDateTime
import org.http4k.lens.StringBiDiMappings.offsetTime
import org.http4k.lens.StringBiDiMappings.regexObject
import org.http4k.lens.StringBiDiMappings.samplingDecision
import org.http4k.lens.StringBiDiMappings.throwable
import org.http4k.lens.StringBiDiMappings.traceId
import org.http4k.lens.StringBiDiMappings.uri
import org.http4k.lens.StringBiDiMappings.url
import org.http4k.lens.StringBiDiMappings.uuid
import org.http4k.lens.StringBiDiMappings.yearMonth
import org.http4k.lens.StringBiDiMappings.zonedDateTime
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.YearMonth
import java.time.ZonedDateTime
import java.util.UUID

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
    text(yearMonth())
    text(localTime())
    text(localDate())
    text(localDateTime())
    text(zonedDateTime())
    text(offsetTime())
    text(offsetDateTime())
    text(eventCategory())
    text(traceId())
    text(samplingDecision())
    text(throwable())
    int({ Status(it, "") }, Status::code)
}

/**
 * This is the set of utility methods which avoid the noise of creating a BiDiMapping when specifying mappings.
 */
inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.text(noinline inFn: (String) -> OUT,
                                                                         noinline outFn: (OUT) -> String) = text(BiDiMapping(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.boolean(noinline inFn: (Boolean) -> OUT,
                                                                            noinline outFn: (OUT) -> Boolean) = boolean(BiDiMapping(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.int(noinline inFn: (Int) -> OUT,
                                                                        noinline outFn: (OUT) -> Int) = int(BiDiMapping(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.long(noinline inFn: (Long) -> OUT,
                                                                         noinline outFn: (OUT) -> Long) = long(BiDiMapping(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.double(noinline outFn: (OUT) -> Double,
                                                                           noinline inFn: (Double) -> OUT) = double(BiDiMapping(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.bigInteger(noinline inFn: (BigInteger) -> OUT,
                                                                               noinline outFn: (OUT) -> BigInteger) = bigInteger(BiDiMapping(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.bigDecimal(noinline inFn: (BigDecimal) -> OUT,
                                                                               noinline outFn: (OUT) -> BigDecimal) = bigDecimal(BiDiMapping(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.uuid(noinline inFn: (UUID) -> OUT,
                                                                         noinline outFn: (OUT) -> UUID) = text(uuid().map(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.uri(noinline inFn: (Uri) -> OUT,
                                                                        noinline outFn: (OUT) -> Uri) = text(uri().map(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.duration(noinline inFn: (Duration) -> OUT,
                                                                             noinline outFn: (OUT) -> Duration) = text(duration().map(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.instant(noinline inFn: (Instant) -> OUT,
                                                                            noinline outFn: (OUT) -> Instant) = text(instant().map(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.localDate(noinline inFn: (LocalDate) -> OUT,
                                                                              noinline outFn: (OUT) -> LocalDate) = text(localDate().map(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.localDateTime(noinline inFn: (LocalDateTime) -> OUT,
                                                                                  noinline outFn: (OUT) -> LocalDateTime) = text(localDateTime().map(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.localTime(noinline inFn: (LocalTime) -> OUT,
                                                                              noinline outFn: (OUT) -> LocalTime) = text(localTime().map(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.offsetDateTime(noinline inFn: (OffsetDateTime) -> OUT,
                                                                                   noinline outFn: (OUT) -> OffsetDateTime) = text(offsetDateTime().map(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.offsetTime(noinline inFn: (OffsetTime) -> OUT,
                                                                               noinline outFn: (OUT) -> OffsetTime) = text(offsetTime().map(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.yearMonth(noinline inFn: (YearMonth) -> OUT,
                                                                              noinline outFn: (OUT) -> YearMonth) = text(yearMonth().map(inFn, outFn))

inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.zonedDateTime(noinline inFn: (ZonedDateTime) -> OUT,
                                                                                  noinline outFn: (OUT) -> ZonedDateTime) = text(zonedDateTime().map(inFn, outFn))

/**
 * Utility method for when only writing/serialization is required
 */
@JvmName("textSerialize")
inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.text(noinline mapping: (OUT) -> String): AutoMappingConfiguration<BUILDER> = text(BiDiMapping(OUT::class.java, { throw java.lang.IllegalArgumentException() }, mapping))

/**
 * Utility method for when only reading/deserialization is required
 */
@JvmName("textDeserialize")
inline fun <BUILDER, reified OUT> AutoMappingConfiguration<BUILDER>.text(noinline mapping: (String) -> OUT): AutoMappingConfiguration<BUILDER> = text(BiDiMapping(OUT::class.java, mapping, { throw java.lang.IllegalArgumentException() }))
