package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.websocket.WsMessage
import org.junit.jupiter.api.Test
import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Period
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZoneOffset.UTC
import java.time.ZonedDateTime
import java.util.Locale
import java.util.UUID
import java.util.concurrent.Executors

class ForyTest {

    private val fory = Fory {
        register<ArbObject>()
        register<CommonJdkPrimitives>()
        register<ZonesAndLocale>()
        register<SpecificMapHolder>()
        register<AnEnum>()
        value(MyValue)
    }

    private val arbObject = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

    @Test
    fun `roundtrips a registered data class`() {
        assertThat(fory.asA<ArbObject>(fory.asBytes(arbObject)), equalTo(arbObject))
    }

    @Test
    fun `roundtrips through a message body`() {
        with(fory) {
            assertThat(Request(GET, "/").binary(arbObject).binary<ArbObject>(), equalTo(arbObject))
        }
    }

    @Test
    fun `roundtrips common jdk and http4k types`() {
        val localDate = LocalDate.of(2000, 1, 1)
        val localTime = LocalTime.of(1, 1, 1)
        val obj = CommonJdkPrimitives(
            Period.of(1, 2, 3),
            Duration.ofMillis(1000),
            localDate,
            localTime,
            LocalDateTime.of(localDate, localTime),
            ZonedDateTime.of(localDate, localTime, ZoneId.of("UTC")),
            OffsetTime.of(localTime, UTC),
            OffsetDateTime.of(localDate, localTime, UTC),
            Instant.EPOCH,
            UUID.fromString("1a448854-1687-4f90-9562-7d527d64383c"),
            Uri.of("http://uri:8000"),
            URI("http://url:9000").toURL(),
            Status.OK
        )

        assertThat(fory.asA(fory.asBytes(obj), CommonJdkPrimitives::class), equalTo(obj))
    }

    @Test
    fun `roundtrips zones and locale`() {
        val obj = ZonesAndLocale(ZoneId.of("America/Toronto"), ZoneOffset.of("-04:00"), Locale.CANADA)

        assertThat(fory.asA(fory.asBytes(obj), ZonesAndLocale::class), equalTo(obj))
    }

    @Test
    fun `roundtrips maps and enums with a registered value type`() {
        val obj = SpecificMapHolder(mapOf(AnEnum.woo to MyValue("foobar")))

        assertThat(fory.asA(fory.asBytes(obj), SpecificMapHolder::class), equalTo(obj))
    }

    @Test
    fun `refuses to marshal an unregistered type`() {
        val unregistered = Fory { register<ArbObject>() }

        assertThat(
            runCatching { unregistered.asBytes(StringHolder("hello")) }.exceptionOrNull(),
            present()
        )
    }

    @Test
    fun `roundtrips through a websocket message`() {
        with(fory) {
            val lens = WsMessage.auto<ArbObject>().toLens()

            assertThat(lens(lens(arbObject)), equalTo(arbObject))
        }
    }

    @Test
    fun `roundtrips concurrently from many threads`() {
        val threads = 16
        val pool = Executors.newFixedThreadPool(threads)

        val results = (1..threads)
            .map { pool.submit<Boolean> { (1..100).all { fory.asA<ArbObject>(fory.asBytes(arbObject)) == arbObject } } }
            .map { it.get() }
        pool.shutdown()

        assertThat(results.all { it }, equalTo(true))
    }

    @Test
    fun `body lens sets a binary content type`() {
        with(fory) {
            assertThat(
                Request(GET, "/").binary(arbObject).header("Content-Type"),
                equalTo(OCTET_STREAM.toHeaderValue())
            )
        }
    }
}
