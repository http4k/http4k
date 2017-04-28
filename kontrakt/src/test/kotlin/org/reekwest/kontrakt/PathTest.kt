package org.reekwest.kontrakt

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Ignore
import org.junit.Test
import org.reekwest.kontrakt.lens.Invalid
import org.reekwest.kontrakt.lens.LensFailure
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class PathTest {

    @Test
    @Ignore // FIXME
    fun `fixed value present`() {
        assertThat(Path.fixed("hello")("hello"), equalTo("world"))
    }

    @Test
    @Ignore // FIXME
    fun `fixed value mismatch`() {
        assertThat(Path.fixed("hello")("world"), absent())
    }

    @Test
    fun `value present`() {
        assertThat(Path.of("hello")("world"), equalTo("world"))
        assertThat(Path.map { it.length }.of("hello")("world"), equalTo(5))
    }

    @Test
    fun `invalid value`() {
        val path = Path.map(String::toInt).of("hello")
        assertThat({ path("world") }, throws(equalTo(LensFailure(Invalid(path)))))
    }

    @Test
    fun `can create a custom type and get it`() {
        val path = Path.map(::MyCustomBodyType).of("bob")
        assertThat(path("hello world!"), equalTo(MyCustomBodyType("hello world!")))
    }

    @Test
    fun `toString is ok`() {
        assertThat(Path.of("hello").toString(), equalTo("Required path 'hello'"))
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
    fun `boolean`() {
        checkContract(Path.boolean(), "true", true)
        checkContract(Path.boolean(), "false", false)
    }

    @Test
    fun `datetime`() = checkContract(Path.dateTime(), "2001-01-01T02:03:04", LocalDateTime.of(2001, 1, 1, 2, 3, 4))

    @Test
    fun `zoned datetime`() = checkContract(Path.zonedDateTime(), "1970-01-01T00:00:00Z[UTC]", ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")))

    private fun <T> checkContract(Path: PathSpec<String, T>, valueAsString: String, tValue: T) {
        val requiredLens = Path.of("hello")
        assertThat(requiredLens(valueAsString), equalTo(tValue))
        assertThat({ requiredLens("hello") }, throws(equalTo(LensFailure(Invalid(requiredLens)))))
    }
}