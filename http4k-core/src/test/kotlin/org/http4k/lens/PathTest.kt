package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Method
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.UriTemplate
import org.http4k.core.with
import org.http4k.routing.RoutedRequest
import org.junit.jupiter.api.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.UUID


class PathTest {

    @Test
    fun `fixed value present`() {
        assertThat(Path.fixed("hello")("hello"), equalTo("hello"))
    }

    @Test
    fun `fixed value mismatch`() {
        assertThat({ (Path.fixed("hello"))("world") }, throws(lensFailureWith(overallType = Failure.Type.Invalid)))
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
        val target = RoutedRequest(Request(Method.GET, "/some/world"), UriTemplate.from("/some/{hello}"))

        assertThat(Path.of("hello")(target), equalTo("world"))
        assertThat(Path.of("hello").extract(target), equalTo("world"))
        assertThat(Path.map { it.length }.of("hello")(target), equalTo(5))
    }

    @Test
    fun `invalid value`() {
        val path = Path.map(String::toInt).of("hello")
        assertThat({ path("world") }, throws(lensFailureWith(Invalid(path.meta), overallType = Failure.Type.Invalid)))
    }

    @Test
    fun `can create a custom type and get it`() {
        val path = Path.map(::MyCustomBodyType).of("bob")
        assertThat(path("hello world!"), equalTo(MyCustomBodyType("hello world!")))
    }

    @Test
    fun `sets value on request uri with proper encoding`() {
        val pathParam = Path.string().of("bob")
        val updated = Request(GET, Uri.of("http://bob.com/first/{bob}/second")).with(pathParam of "123 45/6")
        assertThat(pathParam("123%2045%2F6"), equalTo("123 45/6"))
        assertThat(updated, equalTo(Request(GET, "http://bob.com/first/123%2045%2F6/second")))
    }

    @Test
    fun `toString is ok`() {
        assertThat(Path.of("hello").toString(), equalTo("{hello}"))
    }

    @Test
    fun `int`() = checkContract(Path.int(), "123", 123)

    @Test
    fun `long`() = checkContract(Path.long(), "123", 123)

    @Test
    fun `float`() = checkContract(Path.float(), "123.0", 123f)

    @Test
    fun `double`() = checkContract(Path.double(), "123.0", 123.0)

    @Test
    fun `local date`() = checkContract(Path.localDate(), "2001-01-01", LocalDate.of(2001, 1, 1))

    @Test
    fun `uuid`() = checkContract(Path.uuid(), "f5fc0a3f-ecb5-4ab3-bc75-185165dc4844", UUID.fromString("f5fc0a3f-ecb5-4ab3-bc75-185165dc4844"))

    @Test
    fun `regex`() = checkContract(Path.regex("v(\\d+)", 1), "v123", "123")

    @Test
    fun `boolean`() {
        checkContract(Path.boolean(), "true", true)
        checkContract(Path.boolean(), "false", false)
    }

    @Test
    fun `datetime`() = checkContract(Path.dateTime(), "2001-01-01T02:03:04", LocalDateTime.of(2001, 1, 1, 2, 3, 4))

    @Test
    fun `instant`() = checkContract(Path.instant(), "1970-01-01T00:00:00Z", Instant.EPOCH)

    @Test
    fun `zoned datetime`() = checkContract(Path.zonedDateTime(), "1970-01-01T00:00:00Z[UTC]", ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")))

    private fun <T> checkContract(Path: PathLensSpec<T>, valueAsString: String, tValue: T) {
        val requiredLens = Path.of("hello")
        assertThat(requiredLens(valueAsString), equalTo(tValue))
        assertThat({ requiredLens("hello") }, throws(lensFailureWith(Invalid(requiredLens.meta), overallType = Failure.Type.Invalid)))
    }
}