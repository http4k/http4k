package org.http4k.util

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.Jackson
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.util.UUID

data class ArbObject(
    val string: String,
    val child: ArbObject?,
    val numbers: List<Int>,
    val bool: Boolean?,
    val long: Long?
)

data class SupportedStringPrimitives(
    val duration: Duration,
    val localDate: LocalDate,
    val localTime: LocalTime,
    val localDateTime: LocalDateTime,
    val zonedDateTime: ZonedDateTime,
    val offsetTime: OffsetTime,
    val offsetDateTime: OffsetDateTime,
    val instant: Instant,
    val uuid: UUID,
    val uri: Uri,
    val url: URL
)

data class JsonPrimitives(
    val string: String = "string",
    val boolean: Boolean = false,
    val int: Int = Int.MAX_VALUE,
    val long: Long = Long.MIN_VALUE,
    val double: Double = 9.9999999999,
    val float: Float = -9.9999999999f,
    val bigInt: BigInteger = BigInteger("" + Long.MAX_VALUE + "" + Long.MAX_VALUE),
    val bigDecimal: BigDecimal = BigDecimal("" + Double.MAX_VALUE + "" + 111)
)

@ExtendWith(JsonApprovalTest::class)
class AutoJsonToJsonSchemaTest {
    private val json = Jackson

    private val creator = AutoJsonToJsonSchema(json)

    @Test
    fun `renders schema for various json primitives`(approver: Approver) {
        approver.assertApproved(JsonPrimitives(), "bob")
    }

    private fun Approver.assertApproved(obj: Any, name: String) {
        assertApproved(Response(OK)
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .body(Jackson.asJsonString(creator.toSchema(obj, name))))
    }
}
