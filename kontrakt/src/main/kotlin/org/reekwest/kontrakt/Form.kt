package org.reekwest.kontrakt

import org.reekwest.http.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.Status.Companion.NOT_ACCEPTABLE
import org.reekwest.http.core.body.bodyString
import org.reekwest.http.core.body.toBody
import org.reekwest.http.core.copy
import org.reekwest.http.core.toUrlEncoded
import org.reekwest.http.core.with
import org.reekwest.kontrakt.Header.Common.CONTENT_TYPE
import org.reekwest.kontrakt.lens.BiDiLensSpec
import org.reekwest.kontrakt.lens.Failure
import org.reekwest.kontrakt.lens.Get
import org.reekwest.kontrakt.lens.Invalid
import org.reekwest.kontrakt.lens.Lens
import org.reekwest.kontrakt.lens.LensFailure
import org.reekwest.kontrakt.lens.Set
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
        if (CONTENT_TYPE(target) != APPLICATION_FORM_URLENCODED) throw LensFailure(Invalid(CONTENT_TYPE))
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
