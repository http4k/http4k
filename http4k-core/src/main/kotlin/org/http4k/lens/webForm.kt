package org.http4k.lens

import org.http4k.asString
import org.http4k.core.Body
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.HttpMessage
import org.http4k.core.toUrlFormEncoded
import org.http4k.core.with
import org.http4k.lens.ContentNegotiation.Companion.StrictNoDirective
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.lens.Validator.*
import java.net.URLDecoder.decode

object FormField : BiDiLensSpec<WebForm, String>("formData",
    StringParam,
    LensGet { name, (fields) -> fields.getOrDefault(name, listOf()).filter { it.isNotEmpty() } },
    LensSet { name, values, target -> values.fold(target - name) { m, next -> m + (name to next) } }
)

data class WebForm(val fields: Map<String, List<String>> = emptyMap(), val errors: List<Failure> = emptyList()) {
    operator fun plus(kv: Pair<String, String>): WebForm =
        copy(fields = fields + (kv.first to fields.getOrDefault(kv.first, emptyList()).plus(kv.second)))

    operator fun minus(name: String): WebForm = copy(fields = fields.filterKeys { it != name })
}

/**
 * Convenience function to write the Webform to the message body and set the content type.
 */
fun <R : HttpMessage> R.webForm(t: WebForm): R = with(Body.webForm(Ignore).toLens() of t)

fun Body.Companion.webForm(validator: Validator, vararg formFields: Lens<WebForm, *>): BiDiBodyLensSpec<WebForm> =
    httpBodyRoot(formFields.map { it.meta }, APPLICATION_FORM_URLENCODED, StrictNoDirective)
        .map({ it.payload.asString() }, { Body(it) })
        .map(
            { WebForm(formParametersFrom(it), emptyList()) },
            { (fields) -> fields.flatMap { pair -> pair.value.map { pair.key to it } }.toUrlFormEncoded() })
        .map(
            { it.copy(errors = validator(it, formFields.toList())) },
            { it.copy(errors = validator(it, formFields.toList())) })

private fun formParametersFrom(target: String): Map<String, List<String>> = target
    .split("&")
    .filter { it.contains("=") }
    .map { it.split("=") }
    .map { decode(it[0], "UTF-8") to if (it.size > 1) decode(it[1], "UTF-8") else "" }
    .groupBy { it.first }
    .mapValues { it.value.map { it.second } }

inline fun <reified T : Enum<T>> BiDiLensSpec<WebForm, String>.enum() = map(StringBiDiMappings.enum<T>())
