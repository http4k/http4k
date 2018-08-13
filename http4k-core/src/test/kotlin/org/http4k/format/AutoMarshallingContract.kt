package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID

data class CommonJdkPrimitives(
        val duration: Duration,
        val localDate: LocalDate,
        val localTime: LocalTime,
        val localDateTime: LocalDateTime,
        val zonedDateTime: ZonedDateTime,
        val instant: Instant,
        val uuid: UUID,
        val uri: Uri,
        val url: URL
)

data class ArbObject(val string: String, val child: ArbObject?, val numbers: List<Int>, val bool: Boolean)

data class RegexHolder(val regex: Regex)

abstract class AutoMarshallingContract(private val j: AutoMarshallingJson) {

    protected open val expectedAutoMarshallingResult = """{"string":"hello","child":{"string":"world","child":null,"numbers":[1],"bool":true},"numbers":[],"bool":false}"""
    protected open val expectedAutoMarshallingResultPrimitives = """{"duration":"PT1S","localDate":"2000-01-01","localTime":"01:01:01","localDateTime":"2000-01-01T01:01:01","zonedDateTime":"2000-01-01T01:01:01Z[UTC]","instant":"1970-01-01T00:00:00Z","uuid":"1a448854-1687-4f90-9562-7d527d64383c","uri":"http://uri:8000","url":"http://url:9000"}"""

    val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

    @Test
    fun `roundtrip arbitary object to and from JSON string`() {
        val out = j.asJsonString(obj)
        assertThat(out, equalTo(expectedAutoMarshallingResult))
        assertThat(j.asA(out, ArbObject::class), equalTo(obj))
    }

    @Test
    open fun `fails decoding when a required value is null`() {
        assertThat({ j.asA("{}", ArbObject::class) }, throws<Exception>())
    }

    @Test
    fun `roundtrip object with common java primitive types`() {
        val localDate = LocalDate.of(2000, 1, 1)
        val localTime = LocalTime.of(1, 1, 1)
        val obj = CommonJdkPrimitives(Duration.ofMillis(1000), localDate, localTime, LocalDateTime.of(localDate, localTime), ZonedDateTime.of(localDate, localTime, ZoneId.of("UTC")), Instant.EPOCH, UUID.fromString("1a448854-1687-4f90-9562-7d527d64383c"), Uri.of("http://uri:8000"), URL("http://url:9000"))
        val out = j.asJsonString(obj)
        assertThat(out, equalTo(expectedAutoMarshallingResultPrimitives))
        assertThat(j.asA(out, CommonJdkPrimitives::class), equalTo(obj))
    }

    @Test
    fun `roundtrip regex special as equals isn't comparable`() {
        val obj = RegexHolder(".*".toRegex())
        val out = j.asJsonString(obj)
        assertThat(out, equalTo("""{"regex":".*"}"""))
        assertThat(j.asA(out, RegexHolder::class).regex.pattern, equalTo(obj.regex.pattern))
    }
}