package org.http4k.format

import com.fasterxml.jackson.annotation.JsonPropertyOrder
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.JacksonCsv.auto
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

@JsonPropertyOrder(value = ["string", "numbers", "bool"])
data class CsvArbObject(val string: String, val numbers: List<Int>, val bool: Boolean)

class JacksonCsvBodyTest {

    @Test
    fun `roundtrip list of arbitrary objects to and from body`() {
        val lens = Body.auto<CsvArbObject>().toLens()

        val objects = listOf(
            CsvArbObject("hello", emptyList(), false),
            CsvArbObject("goodbye", listOf(1, 2, 3), true)
        )

        assertThat(
            lens(Response(OK).with(lens of objects)),
            equalTo(objects)
        )
    }

    /**
     * Ignores properties in sub-types.
     * See https://github.com/FasterXML/jackson-dataformats-text/issues/202#issuecomment-657346997
     */
    @Test
    fun `roundtrip list of interface implementations - ignores subclass values`() {
        val lens = Body.auto<Interface>().toLens()
        val objects = listOf(InterfaceImpl(), InterfaceImpl())

        assertThat(
            Response(OK).with(lens of objects).bodyString(),
            equalTo("value\nhello\nhello\n")
        )
    }

    @Test
    fun `roundtrip list of common jdk primitives`() {
        val localDate = LocalDate.of(2000, 1, 1)
        val localTime = LocalTime.of(1, 1, 1)
        val zoneOffset = ZoneOffset.UTC
        val obj = CommonJdkPrimitives(
            Duration.ofMillis(1000),
            localDate,
            localTime,
            LocalDateTime.of(localDate, localTime),
            ZonedDateTime.of(localDate, localTime, ZoneId.of("UTC")),
            OffsetTime.of(localTime, zoneOffset),
            OffsetDateTime.of(localDate, localTime, zoneOffset),
            Instant.EPOCH,
            UUID.fromString("1a448854-1687-4f90-9562-7d527d64383c"),
            Uri.of("http://uri:8000"),
            URI.create("http://url:9000").toURL(),
            OK
        )

        val csv =
            """duration,instant,localDate,localDateTime,localTime,offsetDateTime,offsetTime,status,uri,url,uuid,zonedDateTime
PT1S,1970-01-01T00:00:00Z,2000-01-01,2000-01-01T01:01:01,01:01:01,2000-01-01T01:01:01Z,01:01:01Z,200,http://uri:8000,http://url:9000,"1a448854-1687-4f90-9562-7d527d64383c","2000-01-01T01:01:01Z[UTC]"
"""

        val lens = Body.auto<CommonJdkPrimitives>().toLens()

        assertThat(
            Response(OK).with(lens of listOf(obj)).bodyString(),
            equalTo(csv)
        )
    }
}
