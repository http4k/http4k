package org.http4k.lens

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
        LensGet { _: String, str: String ->
            if (str.isBlank()) emptyList() else listOf(str)
        },
        LensSet { _: String, values: List<String>, str: String -> values.fold(str, { memo, next -> memo + next }) })

    @Test
    fun `int`() = checkContract(spec.int(), "123", 123)

    @Test
    fun `long`() = checkContract(spec.long(), "123", 123)

    @Test
    fun `float`() = checkContract(spec.float(), "123.0", 123f)

    @Test
    fun `double`() = checkContract(spec.double(), "123.0", 123.0)

    @Test
    fun `local date`() = checkContract(spec.localDate(), "2001-01-01", LocalDate.of(2001, 1, 1))

    @Test
    fun `uuid`() = checkContract(spec.uuid(), "f5fc0a3f-ecb5-4ab3-bc75-185165dc4844", UUID.fromString("f5fc0a3f-ecb5-4ab3-bc75-185165dc4844"))

    @Test
    fun `boolean`() {
        checkContract(spec.boolean(), "true", true)
        checkContract(spec.boolean(), "false", false)
    }

    @Test
    fun `datetime`() = checkContract(spec.dateTime(), "2001-01-01T02:03:04", LocalDateTime.of(2001, 1, 1, 2, 3, 4))

    @Test
    fun `zoned datetime`() = checkContract(spec.zonedDateTime(), "1970-01-01T00:00:00Z[UTC]", ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")))

}

