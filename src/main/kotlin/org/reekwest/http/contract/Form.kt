package org.reekwest.http.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.contract.Header.Common.CONTENT_TYPE
import org.reekwest.http.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.reekwest.http.core.Request
import org.reekwest.http.core.body.bodyString
import org.reekwest.http.core.with
import java.net.URLDecoder

typealias FormFields = Map<String, List<String>>

data class WebForm constructor(val fields: Map<String, List<String>>, val errors: List<ExtractionFailure>) {
    operator fun plus(kv: Pair<String, String>): WebForm =
        copy(fields.plus(kv.first to fields.getOrDefault(kv.first, emptyList()).plus(kv.second)))

    fun with(vararg modifiers: (WebForm) -> WebForm): WebForm = modifiers.fold(this, { memo, next -> next(memo) })

    companion object {
        fun emptyForm() = WebForm(emptyMap(), emptyList())
    }
}

enum class FormValidator : (WebForm) -> WebForm {
    Strict {
        override fun invoke(form: WebForm): WebForm = if (form.errors.isEmpty()) form else throw ContractBreach(form.errors)
    },
    Feedback {
        override fun invoke(form: WebForm): WebForm = form
    };
}

fun Body.form() = BiDiBodySpec(BiDiLensSpec<Request, WebForm, WebForm>("body",
    Get { _, target ->
        if (CONTENT_TYPE(target) != APPLICATION_FORM_URLENCODED) throw ContractBreach(Invalid(Header.Common.CONTENT_TYPE))
        listOf(WebForm(formParametersFrom(target), emptyList()))
    },
    Set { _, values, target ->
        values.fold(target, { memo, next ->
            memo.with(required("body") to next.toString().asByteBuffer())
        }).with(CONTENT_TYPE to APPLICATION_FORM_URLENCODED)
    }
)).required()

private fun formParametersFrom(target: Request): Map<String, List<String>> {
    return target.bodyString()
        .split("&")
        .filter { it.contains("=") }
        .map { it.split("=") }
        .map { URLDecoder.decode(it[0], "UTF-8") to if (it.size > 1) URLDecoder.decode(it[1], "UTF-8") else "" }
        .groupBy { it.first }
        .mapValues { it.value.map { it.second } }
}

object FormField : BiDiLensSpec <WebForm, String, String>("form field",
    Get { name, (fields) -> fields.getOrDefault(name, listOf()) },
    Set { name, values, target -> values.fold(target, { m, next -> m.plus(name to next) }) }
)
//
//fun Body.webForm(validator: FormValidator, vararg formFields: BiDiMetaLens<WebForm, *, *>) =
//    BodySpec(LensSpec("form", FormLocator, FormFieldsBiDiMapper.validatingFor(validator, *formFields))).required("form")
//
///** private **/
//
//private object FormFieldsBiDiMapper : BiDiMapper<ByteBuffer, FormFields> {
//    override fun mapIn(source: ByteBuffer): FormFields = String(source.array())
//        .split("&")
//        .filter { it.contains("=") }
//        .map { it.split("=") }
//        .map { decode(it[0], "UTF-8") to if (it.size > 1) decode(it[1], "UTF-8") else "" }
//        .groupBy { it.first }
//        .mapValues { it.value.map { it.second } }
//
//    // FIXME this doesn't serialize properly
//    override fun mapOut(source: FormFields): ByteBuffer = source.toString().asByteBuffer()
//
//    internal fun validatingFor(validator: FormValidator, vararg formFields: BiDiMetaLens<WebForm, *, *>) =
//        FormFieldsBiDiMapper.map({ it ->
//            val formInstance = WebForm(it, emptyList())
//            val failures = formFields.fold(listOf<ExtractionFailure>()) {
//                memo, next ->
//                try {
//                    next(formInstance)
//                    memo
//                } catch (e: ContractBreach) {
//                    memo.plus(e.failures)
//                }
//            }
//            validator(formInstance.copy(errors = failures))
//        }, { it.fields })
//}
