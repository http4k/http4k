package org.reekwest.http.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.contract.ContractBreach.Companion.Invalid
import org.reekwest.http.contract.Header.Common.CONTENT_TYPE
import org.reekwest.http.core.with
import java.net.URLDecoder.decode
import java.nio.ByteBuffer

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

object FormField : LensSpec<WebForm, String>("form field", FormFieldLocator.asByteBuffers(), ByteBufferStringBiDiMapper)

fun Body.webForm(validator: FormValidator, vararg formFields: ContractualLens<WebForm, *, *>) =
    BodySpec(LensSpec("form", FormLocator, FormFieldsBiDiMapper.validatingFor(validator, *formFields))).required("form")

/** private **/

private object FormLocator : (String) -> Lens<HttpMessage, ByteBuffer> {
    override fun invoke(name: String): Lens<HttpMessage, ByteBuffer> =
        object : Lens<HttpMessage, ByteBuffer> {
            override fun invoke(target: HttpMessage): List<ByteBuffer?>? {
                if (CONTENT_TYPE(target) != APPLICATION_FORM_URLENCODED) throw Invalid(CONTENT_TYPE)
                return target.body?.let { listOf(it) } ?: emptyList()
            }

            override fun invoke(values: List<ByteBuffer>, target: HttpMessage): HttpMessage = values
                .fold(target, { memo, next -> memo.with(Body.binary() to next) })
                .with(CONTENT_TYPE to APPLICATION_FORM_URLENCODED)
        }
}

private object FormFieldLocator : (String) -> Lens<WebForm, String> {
    override fun invoke(name: String): Lens<WebForm, String> =
        object : Lens<WebForm, String> {
            override fun invoke(target: WebForm) = target.fields.getOrDefault(name, listOf())
            override fun invoke(values: List<String>, target: WebForm) = values.fold(target, { m, next -> m.plus(name to next) })
        }

}

private object FormFieldsBiDiMapper : BiDiMapper<ByteBuffer, FormFields> {
    override fun mapIn(source: ByteBuffer): FormFields = String(source.array())
        .split("&")
        .filter { it.contains("=") }
        .map { it.split("=") }
        .map { decode(it[0], "UTF-8") to if (it.size > 1) decode(it[1], "UTF-8") else "" }
        .groupBy { it.first }
        .mapValues { it.value.map { it.second } }

    // FIXME this doesn't serialize properly
    override fun mapOut(source: FormFields): ByteBuffer = source.toString().asByteBuffer()

    internal fun validatingFor(validator: FormValidator, vararg formFields: ContractualLens<WebForm, *, *>) =
        FormFieldsBiDiMapper.map({ it ->
            val formInstance = WebForm(it, emptyList())
            val failures = formFields.fold(listOf<ExtractionFailure>()) {
                memo, next ->
                try {
                    next(formInstance)
                    memo
                } catch (e: ContractBreach) {
                    memo.plus(e.failures)
                }
            }
            validator(formInstance.copy(errors = failures))
        }, { it.fields })
}
