package org.http4k.lens


import com.natpryce.hamkrest.MatchResult
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.lens.ParamMeta.StringParam

object BiDiLensContract {

    val spec = BiDiLensSpec("location", StringParam, LensGet { _: String, str: String ->
        if (str.isBlank()) emptyList() else listOf(str)
    },
        LensSet { _: String, values: List<String>, str: String -> values.fold(str, { memo, next -> memo + next }) })

    fun <IN, T> checkContract(spec: BiDiLensSpec<IN, T>, tValue: T, validValue: IN, nullValue: IN, invalidValue: IN, s: IN, modifiedValue: IN, listModifiedValue: IN) {
        //synonym methods
        assertThat(spec.required("hello").extract(validValue), equalTo(tValue))
        assertThat(spec.required("hello").inject(tValue, s), equalTo(modifiedValue))
        assertThat(spec.required("hello")[validValue], equalTo(tValue))
        assertThat(spec.required("hello").set(s, tValue), equalTo(modifiedValue))

        val optionalLens = spec.optional("hello")
        assertThat(optionalLens(validValue), equalTo(tValue))
        assertThat(optionalLens.extract(validValue), equalTo(tValue))
        assertThat((spec.map { it.toString() }.optional("hello"))(validValue), equalTo(tValue.toString()))
        assertThat(optionalLens(nullValue), absent())
        assertThat({ optionalLens(invalidValue) }, throws(lensFailureWith(Invalid(optionalLens.meta), overallType = Failure.Type.Invalid)))
        assertThat(optionalLens(tValue, s), equalTo(modifiedValue))

        val optionalMultiLens = spec.multi.optional("hello")
        assertThat(optionalMultiLens(validValue), equalTo(listOf(tValue)))
        assertThat((spec.map { it.toString() }.multi.optional("hello"))(validValue), equalTo(listOf(tValue.toString())))
        assertThat(optionalMultiLens(nullValue), absent())
        assertThat({ optionalMultiLens(invalidValue) }, throws(lensFailureWith(Invalid(optionalLens.meta), overallType = Failure.Type.Invalid)))
        assertThat(optionalMultiLens(listOf(tValue, tValue), s), equalTo(listModifiedValue))

        val requiredLens = spec.required("hello")
        assertThat(requiredLens(validValue), equalTo(tValue))
        assertThat((spec.map { it.toString() }.required("hello"))(validValue), equalTo(tValue.toString()))
        assertThat({ requiredLens(nullValue) }, throws(lensFailureWith(Missing(requiredLens.meta), overallType = Failure.Type.Missing)))
        assertThat({ requiredLens(invalidValue) }, throws(lensFailureWith(Invalid(requiredLens.meta), overallType = Failure.Type.Invalid)))
        assertThat(requiredLens(tValue, s), equalTo(modifiedValue))

        val requiredMultiLens = spec.multi.required("hello")
        assertThat(requiredMultiLens(validValue), equalTo(listOf(tValue)))
        assertThat((spec.map { it.toString() }.multi.required("hello"))(validValue), equalTo(listOf(tValue.toString())))
        assertThat({ requiredMultiLens(nullValue) }, throws(lensFailureWith(Missing(requiredLens.meta), overallType = Failure.Type.Missing)))
        assertThat({ requiredMultiLens(invalidValue) }, throws(lensFailureWith(Invalid(requiredLens.meta), overallType = Failure.Type.Invalid)))
        assertThat(requiredMultiLens(listOf(tValue, tValue), s), equalTo(listModifiedValue))

        val defaultedLens = spec.defaulted("hello", tValue)
        assertThat(defaultedLens(validValue), equalTo(tValue))
        assertThat((spec.map { it.toString() }.defaulted("hello", "world"))(validValue), equalTo(tValue.toString()))
        assertThat(defaultedLens(nullValue), equalTo(tValue))
        assertThat({ defaultedLens(invalidValue) }, throws(lensFailureWith(Invalid(defaultedLens.meta), overallType = Failure.Type.Invalid)))
        assertThat(defaultedLens(tValue, s), equalTo(modifiedValue))

        val defaultedMultiLens = spec.multi.defaulted("hello", listOf(tValue))
        assertThat(defaultedMultiLens(validValue), equalTo(listOf(tValue)))
        assertThat((spec.map { it.toString() }.multi.defaulted("hello", listOf(tValue.toString())))(validValue), equalTo(listOf(tValue.toString())))
        assertThat(defaultedMultiLens(nullValue), equalTo(listOf(tValue)))
        assertThat({ defaultedMultiLens(invalidValue) }, throws(lensFailureWith(Invalid(defaultedMultiLens.meta), overallType = Failure.Type.Invalid)))
        assertThat(defaultedMultiLens(listOf(tValue, tValue), s), equalTo(listModifiedValue))
    }
}

data class MyCustomBodyType(val value: String)

fun lensFailureWith(vararg failures: Failure, overallType: Failure.Type) = object : Matcher<LensFailure> {
    private val expectedList = failures.toList()
    override val description: String = "LensFailure with type $overallType and failures $expectedList"
    override fun invoke(actual: LensFailure): MatchResult =
        if (actual.failures != expectedList) {
            MatchResult.Mismatch("\n${actual.failures}\ninstead of \n$expectedList")
        } else if (actual.overall() != overallType) {
            MatchResult.Mismatch("${actual.overall()}\ninstead of $overallType")
        } else
            MatchResult.Match
}

inline fun <reified T> targetIsA() = Matcher<LensFailure>("target", { it.target is T })