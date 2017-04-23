package org.reekwest.http.contract

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime

class BiDiLensSpecContract {

    private val spec = BiDiLensSpec("location", Get { _: String, str: String ->
        if (str.isBlank()) emptyList() else listOf(str)
    },
        Set { _: String, values: List<String>, str: String -> values.fold(str, { _, next -> next }) })

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
    fun `boolean`() {
        checkContract(spec.boolean(), "true", true)
        checkContract(spec.boolean(), "false", false)
    }

    @Test
    fun `datetime`() = checkContract(spec.dateTime(), "2001-01-01T02:03:04", LocalDateTime.of(2001, 1, 1, 2, 3, 4))

    @Test
    fun `zoned datetime`() = checkContract(spec.zonedDateTime(), "1970-01-01T00:00:00Z[UTC]", ZonedDateTime.of(1970, 1, 1, 0, 0, 0, 0, ZoneId.of("UTC")))

    private fun <T> checkContract(spec: BiDiLensSpec<String, String, T>, valueAsString: String, tValue: T) {
        val optionalLens = spec.optional("hello")
        assertThat(optionalLens(valueAsString), equalTo(tValue))
        assertThat(optionalLens(""), absent())
        assertThat({ optionalLens("hello") }, throws(equalTo(ContractBreach(Invalid(optionalLens)))))
        assertThat(optionalLens(tValue, "original"), equalTo(valueAsString))

        val requiredLens = spec.required("hello")
        assertThat(requiredLens(valueAsString), equalTo(tValue))
        assertThat({ requiredLens("") }, throws(equalTo(ContractBreach(Missing(requiredLens)))))
        assertThat({ requiredLens("hello") }, throws(equalTo(ContractBreach(Invalid(requiredLens)))))
        assertThat(requiredLens(tValue, "original"), equalTo(valueAsString))
    }


}
