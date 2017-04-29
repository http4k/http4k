package org.reekwest.http.contract

import org.reekwest.http.contract.Header.Common.CONTENT_TYPE
import org.reekwest.http.contract.lens.BiDiLensSpec
import org.reekwest.http.contract.lens.Failure
import org.reekwest.http.contract.lens.Get
import org.reekwest.http.contract.lens.Lens
import org.reekwest.http.contract.lens.LensFailure
import org.reekwest.http.contract.lens.Set
import org.reekwest.http.contract.lens.invalid
import org.reekwest.http.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Status.Companion.NOT_ACCEPTABLE
import org.reekwest.http.core.copy
import org.reekwest.http.core.toBody
import org.reekwest.http.core.toUrlEncoded
import org.reekwest.http.core.with
import java.net.URLDecoder

typealias FormFields = Map<String, List<String>>

object FormField : BiDiLensSpec<WebForm, String, String>("form field",
    Get { name, (fields) -> fields.getOrDefault(name, listOf()) },
    Set { name, values, target -> values.fold(target, { m, next -> m.plus(name to next) }) }
)

data class WebForm constructor(val fields: Map<String, List<String>>, val errors: List<Failure>) {
    operator fun plus(kv: Pair<String, String>): WebForm =
        copy(fields.plus(kv.first to fields.getOrDefault(kv.first, emptyList()).plus(kv.second)))

    fun with(vararg modifiers: (WebForm) -> WebForm): WebForm = modifiers.fold(this, { memo, next -> next(memo) })

    companion object {
        fun emptyForm() = WebForm(emptyMap(), emptyList())
    }
}

enum class FormValidator : (WebForm) -> WebForm {
    Strict {
        override fun invoke(form: WebForm): WebForm = if (form.errors.isEmpty()) form else throw LensFailure(form.errors, NOT_ACCEPTABLE)
    },
    Feedback {
        override fun invoke(form: WebForm): WebForm = form
    };
}

fun Body.webForm(validator: FormValidator, vararg formFields: Lens<WebForm, *>) = BiDiBodySpec(formSpec)
    .map(
        { webForm -> validateFields(webForm, validator, *formFields) },
        { webForm -> validateFields(webForm, validator, *formFields) }
    ).required()

private fun validateFields(webForm: WebForm, validator: FormValidator, vararg formFields: Lens<WebForm, *>): WebForm {
    val failures = formFields.fold(listOf<Failure>()) {
        memo, next ->
        try {
            next(webForm)
            memo
        } catch (e: LensFailure) {
            memo.plus(e.failures)
        }
    }
    return validator(webForm.copy(errors = failures))
}

private val formSpec = BiDiLensSpec<HttpMessage, WebForm, WebForm>("body",
    Get { _, target ->
        if (CONTENT_TYPE(target) != APPLICATION_FORM_URLENCODED) throw LensFailure(CONTENT_TYPE.invalid())
        listOf(WebForm(formParametersFrom(target), emptyList()))
    },
    Set { _, values, target: HttpMessage ->
        values.fold(target, { memo, (fields) ->
            memo.copy(body = fields.flatMap { pair -> pair.value.map { pair.key to it } }.toUrlEncoded().toBody())
        }).with(CONTENT_TYPE to APPLICATION_FORM_URLENCODED)
    }
)

private fun formParametersFrom(target: HttpMessage): Map<String, List<String>> {
    return target.bodyString()
        .split("&")
        .filter { it.contains("=") }
        .map { it.split("=") }
        .map { URLDecoder.decode(it[0], "UTF-8") to if (it.size > 1) URLDecoder.decode(it[1], "UTF-8") else "" }
        .groupBy { it.first }
        .mapValues { it.value.map { it.second } }
}
