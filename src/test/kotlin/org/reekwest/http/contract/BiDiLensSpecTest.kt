package org.reekwest.http.contract

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test

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
