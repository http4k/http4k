package org.reekwest.http.contract

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test

class BiDiLensSpecContract {

    private val spec = BiDiLensSpec("location", Get { _: String, str: String ->
        if (str.isBlank()) emptyList() else listOf(str) },
        Set { str: String, _: List<String>, _: String -> str })

    @Test
    fun `int`() = checkContract(spec.int(), "123", 123)

    @Test
    fun `long`() = checkContract(spec.long(), "123", 123)

    @Test
    fun `float`() = checkContract(spec.float(), "123", 123f)

    @Test
    fun `double`() = checkContract(spec.double(), "123", 123.0)

    private fun <T> checkContract(spec: BiDiLensSpec<String, String, T>, validValue: String, expectedValue: T) {
        val optionalLens = spec.optional("hello")
        assertThat(optionalLens(validValue), equalTo(expectedValue))
        assertThat(optionalLens(""), absent())
        assertThat({ optionalLens("hello") }, throws(equalTo(ContractBreach(Invalid(optionalLens)))))

        val requiredLens = spec.required("hello")
        assertThat(requiredLens(validValue), equalTo(expectedValue))
        assertThat({ requiredLens("") }, throws(equalTo(ContractBreach(Missing(requiredLens)))))
        assertThat({ requiredLens("hello") }, throws(equalTo(ContractBreach(Invalid(requiredLens)))))
    }


}
