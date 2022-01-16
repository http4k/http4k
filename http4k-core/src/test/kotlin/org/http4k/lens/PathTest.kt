package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.base64Encode
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.UriTemplate
import org.http4k.core.with
import org.http4k.routing.RoutedRequest
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class PathTest {

    @Test
    fun `fixed value cannot contain slash characters`() {
        assertThat({ Path.fixed("hel/lo") }, throws<IllegalArgumentException>())
    }

    @Test
    fun `fixed value present`() {
        assertThat(Path.fixed("hello")("hello"), equalTo("hello"))
    }

    @Test
    fun `fixed value mismatch`() {
        assertThat({ (Path.fixed("hello"))("world") }, throws(lensFailureWith<String>(overallType = Failure.Type.Invalid)))
    }

    @Test
    fun `fixed toString`() {
        assertThat(Path.fixed("hello").toString(), equalTo("hello"))
    }

    @Test
    fun `value present`() {
        assertThat(Path.of("hello")("world"), equalTo("world"))
        assertThat(Path.map { it.length }.of("hello")("world"), equalTo(5))
    }

    @Test
    fun `value present in request when it has been pre-parsed`() {
        val target = RoutedRequest(Request(GET, "/some/world"), UriTemplate.from("/some/{hello}"))

        assertThat(Path.of("hello")(target), equalTo("world"))
        assertThat(Path.of("hello").extract(target), equalTo("world"))
        assertThat(Path.map { it.length }.of("hello")(target), equalTo(5))
    }

    @Test
    fun `invalid value`() {
        val path = Path.map(String::toInt).of("hello")
        assertThat({ path("world") }, throws(lensFailureWith<String>(Invalid(path.meta), overallType = Failure.Type.Invalid)))
    }

    @Test
    fun `can create a custom type and get it`() {
        val path = Path.map(::MyCustomType).of("bob")
        assertThat(path("hello world!"), equalTo(MyCustomType("hello world!")))
    }

    @Test
    fun `sets value on request uri with proper encoding`() {
        fun checkEncodeDecode(unencoded: String, encoded: String) {
            val pathParam = Path.of("bob")
            val updated = RoutedRequest(Request(GET, Uri.of("http://bob.com/first/{bob}/second")).with(pathParam of unencoded), UriTemplate.from("/first/{bob}/second"))
            assertThat(updated, equalTo(Request(GET, "http://bob.com/first/$encoded/second")))
            assertThat(pathParam(updated), equalTo(unencoded))
        }

        fun checkDecode(encoded: String, unencoded: String) {
            val pathParam = Path.of("bob")
            val updated = RoutedRequest(Request(GET, Uri.of("http://bob.com/first/$encoded/second")), UriTemplate.from("/first/{bob}/second"))
            assertThat(pathParam(updated), equalTo(unencoded))
        }

        // unreserved
        checkEncodeDecode("azAZ09-._~", "azAZ09-._~")
        // subdelimiter
        checkEncodeDecode("!$&'()*+,;=", "!$&'()*+,;=")
        // others
        checkEncodeDecode(":@", ":@")
        checkEncodeDecode("Bob Tables%/M", "Bob%20Tables%25%2FM")
        checkEncodeDecode("2020-03-19T19:12:34.567+01:00", "2020-03-19T19:12:34.567+01:00")
        checkEncodeDecode("ÅÄÖ", "%C3%85%C3%84%C3%96")
        checkDecode("Bob%20Tables%25%2FM", "Bob Tables%/M")
        checkDecode("ÅÄÖ", "ÅÄÖ")
    }

    @Test
    fun `toString is ok`() {
        assertThat(Path.of("hello").toString(), equalTo("{hello}"))
    }

    @Test
    fun nonEmptyString() {
        val requiredLens = Path.nonEmptyString().of("hello")
        assertThat(requiredLens("123"), equalTo("123"))
        assertThat({ requiredLens("") }, throws(lensFailureWith<String>(Invalid(requiredLens.meta), overallType = Failure.Type.Invalid)))
    }

    @Test
    fun int() = checkContract(Path.int(), "123", 123)

    @Test
    fun long() = checkContract(Path.long(), "123", 123)

    @Test
    fun float() = checkContract(Path.float(), "123.0", 123f)

    @Test
    fun double() = checkContract(Path.double(), "123.0", 123.0)

    @Test
    fun bigInteger() = checkContract(Path.bigInteger(), "100", BigInteger.valueOf(100))

    @Test
    fun bigDecimal() = checkContract(Path.bigDecimal(), "100", BigDecimal(100))

    @Test
    fun uuid() = checkContract(Path.uuid(), "f5fc0a3f-ecb5-4ab3-bc75-185165dc4844", UUID.fromString("f5fc0a3f-ecb5-4ab3-bc75-185165dc4844"))

    @Test
    fun regex() = checkContract(Path.regex("v(\\d+)", 1), "v123", "123")

    @Test
    fun boolean() {
        checkContract(Path.boolean(), "true", true)
        checkContract(Path.boolean(), "false", false)
    }

    @Test
    fun datetime() = checkContract(Path.dateTime(), "2001-01-01T02:03:04", LocalDateTime.of(2001, 1, 1, 2, 3, 4))

    @Test
    fun base64() = checkContract(Path.base64(), "unencoded".base64Encode(), "unencoded")

    @Test
    fun instant() = checkContract(Path.instant(), "1970-01-01T00:00:00Z", Instant.EPOCH)

    @Test
    fun `local date`() = checkContract(Path.localDate(), "2001-01-01", LocalDate.of(2001, 1, 1))

    @Test
    fun `local time`() = checkContract(Path.localTime(), "01:01:01", LocalTime.of(1, 1, 1))

    @Test
    fun `zoned datetime`() = checkContract(Path.zonedDateTime(), "1970-01-01T00:00:00Z[UTC]", ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")))

    @Test
    fun `enum`() = checkContract(Path.enum(), "DELETE", DELETE)

    @Test
    fun `mapped enum`() {
        val requiredLens = Path.enum(MappedEnum::from, MappedEnum::to).of("whatevs")
        assertThat(requiredLens("eulav"), equalTo(MappedEnum.value))
    }

    @Test
    fun value() {
        checkContract(Path.value(MyInt), "123", MyInt.of(123))
        checkContract(Path.value(MyUUID), UUID(0, 0).toString(), MyUUID.of(UUID(0, 0)))
    }

    private fun <T> checkContract(Path: PathLensSpec<T>, valueAsString: String, tValue: T) {
        val requiredLens = Path.of("hello")
        assertThat(requiredLens(valueAsString), equalTo(tValue))
        assertThat({ requiredLens("hello") }, throws(lensFailureWith<String>(Invalid(requiredLens.meta), overallType = Failure.Type.Invalid)))
    }
}
