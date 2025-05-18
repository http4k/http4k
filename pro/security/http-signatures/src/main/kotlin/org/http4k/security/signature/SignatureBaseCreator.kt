package org.http4k.security.signature

import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.allValues
import dev.forkhandles.result4k.map
import org.http4k.core.HttpMessage
import org.http4k.core.Request

/**
 * Creates a signature base string from an HTTP message, a list of signature components, and signature parameters.
 *
 * @param Target The type of the HTTP message to be signed.
 */
fun interface SignatureBaseCreator<Target : HttpMessage> {
    operator fun invoke(
        request: Request,
        target: Target,
        components: List<SignatureComponent<Target>>,
        params: SignatureParameters
    ): Result<SignatureValue, ExtractorError>

    companion object {
        /**
         * Creates a default signature base string from the given message, components, and parameters.
         *
         * @return A SignatureBaseCreator that generates a default signature base string.
         */
        fun <Target : HttpMessage> Default() = SignatureBaseCreator<Target> { request, target, components, params ->
            val componentsResults = components.map { component ->
                component(request, target)
                    .map { "\"${formatComponentName(component)}\": $it" }
            }

            componentsResults.allValues()
                .map { (it + components.createParamsLine(params)).joinToString("\n") }
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
