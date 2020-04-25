package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.base64Encode
import org.http4k.core.Method
import org.http4k.core.Uri
import org.http4k.lens.BiDiLensContract.checkContract
import org.http4k.lens.ParamMeta.StringParam
import org.junit.jupiter.api.Test
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
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID


class BiDiLensSpecTest {

    data class Container(val s: String?)

    private val spec = BiDiLensSpec("location", StringParam,
        LensGet { _: String, str: String ->
            if (str.isBlank()) emptyList() else listOf(str)
        },
        LensSet { _: String, values: List<String>, str: String -> values.fold(str) { memo, next -> memo + next } })

    private val oSpec = BiDiLensSpec("location", StringParam,
        LensGet { _: String, (s) -> s?.let(::listOf) ?: emptyList() },
        LensSet { _: String, values: List<String>, str: Container -> values.fold(str) { (value), next -> Container(value + next) } })

    @Test
    fun nonEmptyString() = checkContract(oSpec.nonEmptyString(), "123", Container("123"), Container(null), Container(""), Container("o"), Container("o123"), Container("o123123"))

    @Test
    fun int() = checkContract(spec.int(), 123, "123", "", "invalid", "o", "o123", "o123123")

    @Test
    fun long() = checkContract(spec.long(), 123, "123", "", "invalid", "o", "o123", "o123123")

    @Test
    fun float() = checkContract(spec.float(), 123f, "123.0", "", "invalid", "o", "o123.0", "o123.0123.0")

    @Test
    fun double() = checkContract(spec.double(), 123.0, "123.0", "", "invalid", "o", "o123.0", "o123.0123.0")

    @Test
    fun bigDecimal() = checkContract(spec.bigDecimal(), BigDecimal("123.0"), "123.0", "", "invalid", "o", "o123.0", "o123.0123.0")

    @Test
    fun base64() {
        checkContract(spec.base64(), "unencoded", "unencoded".base64Encode(), "", "hello", "unencoded", "unencodeddW5lbmNvZGVk", "unencodeddW5lbmNvZGVkdW5lbmNvZGVk")
    }

    @Test
    fun bigInteger() = checkContract(spec.bigInteger(), BigInteger("123"), "123", "", "invalid", "o", "o123", "o123123")

    @Test
    fun `local date`() = checkContract(spec.localDate(), LocalDate.of(2001, 1, 1), "2001-01-01", "", "123", "o", "o2001-01-01", "o2001-01-012001-01-01")

    @Test
    fun `local time`() = checkContract(spec.localTime(), LocalTime.of(1, 1, 1), "01:01:01", "", "123", "o", "o01:01:01", "o01:01:0101:01:01")

    @Test
    fun `offset time`() = checkContract(spec.offsetTime(), OffsetTime.of(1, 1, 1, 0, ZoneOffset.UTC), "01:01:01Z", "", "123", "o", "o01:01:01Z", "o01:01:01Z01:01:01Z")

    @Test
    fun `offset date time`() = checkContract(spec.offsetDateTime(), OffsetDateTime.of(LocalDate.of(2001, 1, 1), LocalTime.of(1, 1, 1), ZoneOffset.UTC), "2001-01-01T01:01:01Z", "", "123", "o", "o2001-01-01T01:01:01Z", "o2001-01-01T01:01:01Z2001-01-01T01:01:01Z")

    @Test
    fun uuid() = checkContract(spec.uuid(), UUID.fromString("f5fc0a3f-ecb5-4ab3-bc75-185165dc4844"), "f5fc0a3f-ecb5-4ab3-bc75-185165dc4844", "", "123", "o", "of5fc0a3f-ecb5-4ab3-bc75-185165dc4844", "of5fc0a3f-ecb5-4ab3-bc75-185165dc4844f5fc0a3f-ecb5-4ab3-bc75-185165dc4844")

    @Test
    fun regex() {
        val requiredLens = spec.regex("v(\\d+)", 1).required("hello")
        assertThat(requiredLens("v123"), equalTo("123"))
        assertThat((spec.regex("v(\\d+)", 1).map(String::toInt).required("hello"))("v123"), equalTo(123))
        assertThat({ requiredLens("hello") }, throws(lensFailureWith<String>(Invalid(requiredLens.meta), overallType = Failure.Type.Invalid)))
    }

    @Test
    fun boolean() {
        checkContract(spec.boolean(), true, "true", "", "123", "o", "otrue", "otruetrue")
        checkContract(spec.boolean(), false, "false", "", "123", "o", "ofalse", "ofalsefalse")
    }

    @Test
    fun datetime() = checkContract(spec.dateTime(), LocalDateTime.of(2001, 1, 1, 2, 3, 4), "2001-01-01T02:03:04", "", "123", "o", "o2001-01-01T02:03:04", "o2001-01-01T02:03:042001-01-01T02:03:04")

    @Test
    fun instant() = checkContract(spec.instant(), Instant.EPOCH, "1970-01-01T00:00:00Z", "", "123", "o", "o1970-01-01T00:00:00Z", "o1970-01-01T00:00:00Z1970-01-01T00:00:00Z")

    @Test
    fun `zoned datetime`() = checkContract(spec.zonedDateTime(), ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), "1970-01-01T00:00:00Z[UTC]", "", "123", "o", "o1970-01-01T00:00:00Z[UTC]", "o1970-01-01T00:00:00Z[UTC]1970-01-01T00:00:00Z[UTC]")

    @Test
    fun duration() = checkContract(spec.duration(), Duration.ofSeconds(35), "PT35S", "", "notathing", "o", "oPT35S", "oPT35SPT35S")

    @Test
    fun uri() = checkContract(spec.uri(), Uri.of("http://localhost"), "http://localhost", "", null, "o", "ohttp://localhost", "ohttp://localhosthttp://localhost")

    @Test
    fun yearMonth() = checkContract(spec.yearMonth(), YearMonth.of(2000, 2), "2000-02", "", "invalid", "o", "o2000-02", "o2000-022000-02")

    @Test
    fun enum() {
        fun BiDiLensSpec<String, String>.enum() = map(StringBiDiMappings.enum<Method>())
        checkContract(spec.enum(), Method.DELETE, "DELETE", "", "invalid", "o", "oDELETE", "oDELETEDELETE")
    }


    @Test
    fun bytes() {
        val requiredLens = spec.bytes().required("hello")
        assertThat(String(requiredLens("123")), equalTo("123"))
        assertThat({ requiredLens("") }, throws(lensFailureWith<String>(Missing(requiredLens.meta), overallType = Failure.Type.Missing)))
    }

    @Test
    fun `can composite object from several sources and decompose it again`() {
        data class CompositeObject(val number: Int, val string: String?)

        val lens = spec.composite(
            { CompositeObject(int().required("")(it), required("")(it)) },
            { it + number + string }
        )
        val expected = CompositeObject(123, "123")
        assertThat(lens("123"), equalTo(expected))
        assertThat(lens(expected, "prefix"), equalTo("prefix123123"))
    }
}
