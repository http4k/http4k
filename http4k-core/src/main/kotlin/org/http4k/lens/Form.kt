package org.http4k.lens

import org.http4k.asByteBuffer
import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.Status.Companion.NOT_ACCEPTABLE
import org.http4k.core.toUrlEncoded
import org.http4k.lens.ContentNegotiation.Strict
import org.http4k.lens.ParamMeta.StringParam
import java.net.URLDecoder.decode
import java.nio.ByteBuffer

typealias FormFields = Map<String, List<String>>

object FormField : BiDiLensSpec<WebForm, String, String>("form field",
    StringParam,
    LensGet { name, (fields) -> fields.getOrDefault(name, listOf()) },
    LensSet { name, values, target -> values.fold(target, { m, next -> m.plus(name to next) }) }
)

data class WebForm constructor(val fields: Map<String, List<String>> = emptyMap(), val errors: List<Failure> = emptyList()) {
    operator fun plus(kv: Pair<String, String>): WebForm =
        copy(fields.plus(kv.first to fields.getOrDefault(kv.first, emptyList()).plus(kv.second)))

    fun with(vararg modifiers: (WebForm) -> WebForm): WebForm = modifiers.fold(this, { memo, next -> next(memo) })
}

enum class FormValidator : (WebForm) -> WebForm {
    Strict {
        override fun invoke(form: WebForm): WebForm = if (form.errors.isEmpty()) form else throw LensFailure(form.errors, NOT_ACCEPTABLE)
    },
    Feedback {
        override fun invoke(form: WebForm): WebForm = form
    };
}

fun Body.Companion.webForm(validator: FormValidator, vararg formFields: Lens<WebForm, *>): BiDiBodyLens<WebForm> =
    root(formFields.map { it.meta }, APPLICATION_FORM_URLENCODED, Strict)
        .map(ByteBuffer::asString, String::asByteBuffer)
        .map(
            { WebForm(formParametersFrom(it), emptyList()) },
            { (fields) -> fields.flatMap { pair -> pair.value.map { pair.key to it } }.toUrlEncoded() })
        .map({ validateFields(it, validator, *formFields) },
            { validateFields(it, validator, *formFields) })
        .toLens()


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

private fun formParametersFrom(target: String): Map<String, List<String>> {
    return target
        .split("&")
        .filter { it.contains("=") }
        .map { it.split("=") }
        .map { decode(it[0], "UTF-8") to if (it.size > 1) decode(it[1], "UTF-8") else "" }
        .groupBy { it.first }
        .mapValues { it.value.map { it.second } }
}
