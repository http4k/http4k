package org.http4k.lens

import com.natpryce.hamkrest.MatchResult.Match
import com.natpryce.hamkrest.MatchResult.Mismatch
import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.and
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.lens.ParamMeta.ArrayParam
import org.http4k.lens.ParamMeta.StringParam

object BiDiLensContract {

    val spec = BiDiLensSpec("location", StringParam, LensGet { _: String, str: String ->
        if (str.isBlank()) emptyList() else listOf(str)
    },
        LensSet { _: String, values: List<String>, str: String -> values.fold(str) { memo, next -> memo + next } })

    inline fun <reified IN : Any, T> checkContract(spec: BiDiLensSpec<IN, T>, tValue: T, validValue: IN, nullValue: IN, invalidValue: IN?, unmodifiedValue: IN, modifiedValue: IN, listModifiedValue: IN) {
        //synonym methods
        assertThat(spec.required("hello").inject(tValue, unmodifiedValue), equalTo(modifiedValue))
        assertThat(spec.required("hello").extract(validValue), equalTo(tValue))
        assertThat(spec.required("hello")[validValue], equalTo(tValue))
        assertThat(spec.required("hello").set(unmodifiedValue, tValue), equalTo(modifiedValue))

        val optionalLens = spec.optional("hello")
        assertThat(optionalLens(validValue), equalTo(tValue))
        assertThat(optionalLens.extract(validValue), equalTo(tValue))
        assertThat((spec.map { it.toString() }.optional("hello"))(validValue), equalTo(tValue.toString()))
        assertThat(optionalLens(nullValue), absent())
        invalidValue?.let {
            assertThat("expecting to invalid with optional", { optionalLens(invalidValue) }, throws(lensFailureWith<IN>(Invalid(optionalLens.meta), overallType = Failure.Type.Invalid)))
        }
        assertThat(optionalLens(tValue, unmodifiedValue), equalTo(modifiedValue))

        val optionalMultiLens = spec.multi.optional("hello")
        assertThat(optionalMultiLens(validValue), equalTo(listOf(tValue)))
        assertThat((spec.map { it.toString() }.multi.optional("hello"))(validValue), equalTo(listOf(tValue.toString())))
        assertThat(optionalMultiLens(nullValue), absent())
        invalidValue?.let {
            assertThat({ optionalMultiLens(invalidValue) }, throws(lensFailureWith<IN>(Invalid(optionalLens.meta.copy(paramMeta = ArrayParam(optionalLens.meta.paramMeta))), overallType = Failure.Type.Invalid)))
        }
        assertThat(optionalMultiLens(listOf(tValue, tValue), unmodifiedValue), equalTo(listModifiedValue))

        val requiredLens = spec.required("hello")
        assertThat(requiredLens(validValue), equalTo(tValue))
        assertThat((spec.map { it.toString() }.required("hello"))(validValue), equalTo(tValue.toString()))
        assertThat({ requiredLens(nullValue) }, throws(lensFailureWith<IN>(Missing(requiredLens.meta), overallType = Failure.Type.Missing)))
        invalidValue?.let {
            assertThat({ requiredLens(invalidValue) }, throws(lensFailureWith<IN>(Invalid(requiredLens.meta), overallType = Failure.Type.Invalid)))
        }
        assertThat(requiredLens(tValue, unmodifiedValue), equalTo(modifiedValue))

        val requiredMultiLens = spec.multi.required("hello")
        assertThat(requiredMultiLens(validValue), equalTo(listOf(tValue)))
        assertThat((spec.map { it.toString() }.multi.required("hello"))(validValue), equalTo(listOf(tValue.toString())))
        assertThat({ requiredMultiLens(nullValue) }, throws(lensFailureWith<IN>(Missing(requiredMultiLens.meta.copy(paramMeta = ArrayParam(requiredLens.meta.paramMeta))), overallType = Failure.Type.Missing)))
        invalidValue?.let {
            assertThat({ requiredMultiLens(invalidValue) }, throws(lensFailureWith<IN>(Invalid(requiredMultiLens.meta.copy(paramMeta = ArrayParam(requiredLens.meta.paramMeta))), overallType = Failure.Type.Invalid)))
        }
        assertThat(requiredMultiLens(listOf(tValue, tValue), unmodifiedValue), equalTo(listModifiedValue))

        val defaultedLens = spec.defaulted("hello", tValue)
        assertThat(defaultedLens(validValue), equalTo(tValue))
        assertThat((spec.map { it.toString() }.defaulted("hello", "world"))(validValue), equalTo(tValue.toString()))
        assertThat(defaultedLens(nullValue), equalTo(tValue))
        invalidValue?.let {
            assertThat({ defaultedLens(invalidValue) }, throws(lensFailureWith<IN>(Invalid(defaultedLens.meta), overallType = Failure.Type.Invalid)))
        }
        assertThat(defaultedLens(tValue, unmodifiedValue), equalTo(modifiedValue))

        val defaultedMultiLens = spec.multi.defaulted("hello", listOf(tValue))
        assertThat(defaultedMultiLens(validValue), equalTo(listOf(tValue)))
        assertThat((spec.map { it.toString() }.multi.defaulted("hello", listOf(tValue.toString())))(validValue), equalTo(listOf(tValue.toString())))
        assertThat(defaultedMultiLens(nullValue), equalTo(listOf(tValue)))
        invalidValue?.let {
            assertThat({ defaultedMultiLens(invalidValue) }, throws(lensFailureWith<IN>(Invalid(defaultedMultiLens.meta), overallType = Failure.Type.Invalid)))
        }
        assertThat(defaultedMultiLens(listOf(tValue, tValue), unmodifiedValue), equalTo(listModifiedValue))
    }
}

data class MyCustomType(val value: String)
data class MyCustomNullableType(val value: String?)

inline fun <reified T> lensFailureWith(vararg failures: Failure, overallType: Failure.Type) = object : Matcher<LensFailure> {
    private val expectedList = failures.toList()
    override val description: String = "LensFailure with type $overallType and failures $expectedList"
    override fun invoke(actual: LensFailure) = when {
        actual.failures != expectedList -> Mismatch("\n${actual.failures}\ninstead of \n$expectedList")
        actual.overall() != overallType -> Mismatch("${actual.overall()}\ninstead of $overallType")
        else -> Match
    }
}.and(targetIsA<T>())

inline fun <reified T> targetIsA() = Matcher<LensFailure>("target is a " + T::class.qualifiedName) { it.target is T }
