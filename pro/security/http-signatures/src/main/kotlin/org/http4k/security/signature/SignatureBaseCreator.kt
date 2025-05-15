package org.http4k.security.signature

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.allValues
import dev.forkhandles.result4k.map
import org.http4k.core.HttpMessage

/**
 * Creates a signature base string from an HTTP message, a list of signature components, and signature parameters.
 *
 * @param M The type of the HTTP message.
 */
fun interface SignatureBaseCreator<M : HttpMessage> {
    operator fun invoke(message: M, components: List<SignatureComponent<M>>, params: SignatureParameters):
        Result<SignatureValue, ExtractorError>

    companion object {
        /**
         * Creates a default signature base string from the given message, components, and parameters.
         *
         * @param M The type of the HTTP message.
         * @return A SignatureBaseCreator that generates a default signature base string.
         */
        fun <M : HttpMessage> Default() = SignatureBaseCreator<M> { message, components, params ->
            val componentsResults = components.map { component ->
                component(message)
                    .map { "\"${formatComponentName(component)}\": $it" }
            }

            componentsResults.allValues().map { (it + components.createParamsLine(params)).joinToString("\n") }
        }
    }
}

private fun List<SignatureComponent<*>>.createParamsLine(params: SignatureParameters): String {
    val componentsList = joinToString(" ") { "\"${formatComponentName(it)}\"" }
    val paramsList = buildList {
        add("($componentsList)")
        params.created?.let { add("created=${it.epochSecond}") }
        params.expires?.let { add("expires=${it.epochSecond}") }
        add("keyid=\"${params.keyId}\"")
        add("alg=\"${params.algorithm}\"")
        params.nonce?.let { add("nonce=\"${params.nonce}\"") }
        params.tag?.let { add("tag=\"${params.tag}\"") }
    }.joinToString(";")

    return "\"@signature-params\": $paramsList"
}
