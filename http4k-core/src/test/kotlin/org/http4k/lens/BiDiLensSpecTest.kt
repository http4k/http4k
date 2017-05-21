package org.http4k.lens

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.lens.BiDiLensContract.checkContract
import org.http4k.lens.ParamMeta.StringParam
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.*

class BiDiLensSpecTest {

    private val spec = BiDiLensSpec("location", StringParam,
        LensGet { _: String, (s) -> s?.let(::listOf) ?: emptyList() },
        LensSet { _: String, values: List<String>, str: Container -> values.fold(str, { (value), next -> Container(value + next) }) })

    @Test
    fun `nonEmptyString`() = checkContract(spec.nonEmptyString(), "123", Container("123"), Container(null), Container(""), Container("o"), Container("o123"), Container("o123123"))

    @Test
    fun `int`() = checkContract(spec.int(), 123, Container("123"), Container(null), Container("invalid"), Container("o"), Container("o123"), Container("o123123"))

    @Test
    fun `long`() = checkContract(spec.long(), 123, Container("123"), Container(null), Container("invalid"), Container("o"), Container("o123"), Container("o123123"))

    @Test
    fun `float`() = checkContract(spec.float(), 123f, Container("123.0"), Container(null), Container("invalid"), Container("o"), Container("o123.0"), Container("o123.0123.0"))

    @Test
    fun `double`() = checkContract(spec.double(), 123.0, Container("123.0"), Container(null), Container("invalid"), Container("o"), Container("o123.0"), Container("o123.0123.0"))

    @Test
    fun `local date`() = checkContract(spec.localDate(), LocalDate.of(2001, 1, 1), Container("2001-01-01"), Container(null), Container("123"), Container("o"), Container("o2001-01-01"), Container("o2001-01-012001-01-01"))

    @Test
    fun `uuid`() = checkContract(spec.uuid(), UUID.fromString("f5fc0a3f-ecb5-4ab3-bc75-185165dc4844"), Container("f5fc0a3f-ecb5-4ab3-bc75-185165dc4844"), Container(null), Container("123"), Container("o"), Container("of5fc0a3f-ecb5-4ab3-bc75-185165dc4844"), Container("of5fc0a3f-ecb5-4ab3-bc75-185165dc4844f5fc0a3f-ecb5-4ab3-bc75-185165dc4844"))

    @Test
    fun `regex`() {
        val requiredLens = spec.regex("v(\\d+)", 1).required("hello")
        assertThat(requiredLens(Container("v123")), equalTo("123"))
        assertThat((spec.regex("v(\\d+)", 1).map(String::toInt).required("hello"))(Container("v123")), equalTo(123))
        assertThat({ requiredLens(Container("hello")) }, throws(equalTo(LensFailure(requiredLens.invalid()))))
    }

    @Test
    fun `boolean`() {
        checkContract(spec.boolean(), true, Container("true"), Container(null), Container("123"), Container("o"), Container("otrue"), Container("otruetrue"))
        checkContract(spec.boolean(), false, Container("false"), Container(null), Container("123"), Container("o"), Container("ofalse"), Container("ofalsefalse"))
    }

    @Test
    fun `datetime`() = checkContract(spec.dateTime(), LocalDateTime.of(2001, 1, 1, 2, 3, 4), Container("2001-01-01T02:03:04"), Container(null), Container("123"), Container("o"), Container("o2001-01-01T02:03:04"), Container("o2001-01-01T02:03:042001-01-01T02:03:04"))

    @Test
    fun `zoned datetime`() = checkContract(spec.zonedDateTime(), ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")), Container("1970-01-01T00:00:00Z[UTC]"), Container(null), Container("123"), Container
    ("o"), Container("o1970-01-01T00:00:00Z[UTC]"), Container("o1970-01-01T00:00:00Z[UTC]1970-01-01T00:00:00Z[UTC]"))

}

