package org.http4k.security.signature

import org.http4k.core.HttpMessage
import org.http4k.lens.LensExtractor
import org.http4k.security.Nonce
import org.http4k.security.signature.SignatureComponent.Header
import java.time.Instant

/**
 * Extracts out components from the [Signature-Input] header.
 */
class SignatureInputParser<Target : HttpMessage>(private val componentsFactory: SignatureComponentFactory<Target>) :
    LensExtractor<Target, List<SignatureInput<Target>>?> {
    override operator fun invoke(target: Target) = target.header("Signature-Input")?.let {
        (target.header("Signature-Input") ?: "").split(",")
            .map(String::trim)
            .mapNotNull { part ->
                val (labelPart, valuePart) = part.split("=", limit = 2).map { it.trim() }
                parseSignatureInputValue(labelPart.trim(), valuePart)
            }
    }

    private fun parseSignatureInputValue(label: String, value: String): SignatureInput<Target>? = when {
        !value.startsWith("(") || !value.contains(")") -> null
        else -> {
            val componentsEnd = value.indexOf(')')
            val componentsListString = value.substring(1, componentsEnd).trim()
            val components = parseComponentsList(componentsListString)

            val parametersString = when {
                componentsEnd + 1 < value.length -> value.substring(componentsEnd + 1).trim()
                else -> ""
            }

            SignatureInput(label, components, parseParameters(parametersString))
        }
    }

    private fun parseComponentsList(componentsString: String): List<SignatureComponent<Target>> =
        when {
            componentsString.isEmpty() -> emptyList()
            else -> {
                val components = mutableListOf<SignatureComponent<Target>>()
                var insideQuotes = false
                var currentComponent = StringBuilder()

                for (char in componentsString) {
                    when {
                        char == '"' -> {
                            insideQuotes = !insideQuotes
                            currentComponent.append(char)
                        }

                        char == ' ' && !insideQuotes -> {
                            val comp = currentComponent.toString().trim()
                            if (comp.isNotEmpty()) {
                                parseComponent(comp).let { components.add(it) }
                                currentComponent = StringBuilder()
                            }
                        }

                        else -> currentComponent.append(char)
                    }
                }

                val lastComp = currentComponent.toString().trim()
                if (lastComp.isNotEmpty()) parseComponent(lastComp).let(components::add)

                components
            }
        }

    private fun parseComponent(componentString: String): SignatureComponent<Target> {
        val unquoted = componentString.trim('"')

        val parts = unquoted.split(";")
        val name = parts[0].trim()

        val params = when {
            parts.size > 1 -> parts.subList(1, parts.size)
                .associate { param ->
                    val paramParts = param.split("=", limit = 2)
                    paramParts[0].trim() to when {
                        paramParts.size > 1 -> paramParts[1].trim('"', ' ')
                        else -> ""
                    }
                }

            else -> emptyMap()
        }

        return componentsFactory(name, params) ?: Header(name, params)
    }

    private fun parseParameters(parametersString: String): SignatureParameters = when {
        parametersString.isEmpty() -> SignatureParameters(keyId = "", algorithm = "")

        else -> {
            val params = parametersString.split(";")
                .associate { param ->
                    val parts = param.split("=", limit = 2)
                    val name = parts[0].trim()
                    val value = if (parts.size > 1) parts[1].trim('"', ' ') else ""
                    name to value
                }

            SignatureParameters(
                keyId = params["keyid"] ?: "",
                algorithm = params["alg"] ?: "",
                created = params["created"]?.toLongOrNull()?.let(Instant::ofEpochSecond),
                expires = params["expires"]?.toLongOrNull()?.let(Instant::ofEpochSecond),
                nonce = params["nonce"]?.let(::Nonce),
                tag = params["tag"]
            )
        }
    }
}
