package org.http4k.openapi.v3

import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.openapi.NamedSchema
import org.http4k.openapi.namedSchema

/**
 * Convenience type for working with generated code
 */
data class PathV3(val urlPathPattern: String, val method: Method, val pathV3Spec: PathV3Spec) {
    val uniqueName = (pathV3Spec.operationId
        ?: method.toString().toLowerCase() + urlPathPattern.replace('/', '_')).capitalize()

    private fun modelName(contentType: String, suffix: String) =
        uniqueName + ContentType(contentType).value.substringAfter('/').capitalize().filter(Char::isLetterOrDigit) + suffix

    fun requestSchemas(): List<NamedSchema> =
        listOfNotNull(pathV3Spec.requestBody?.content?.entries
            ?.mapNotNull { (contentType, messageSpec) ->
                messageSpec.schema?.namedSchema(modelName(contentType, "Request"))
            }
        ).flatten()

    fun responseSchemas(): List<NamedSchema> = pathV3Spec.responses.entries
        .flatMap { (code, messageSpec) ->
            messageSpec.content.entries.mapNotNull { (contentType, messageSpec) ->
                messageSpec.schema?.namedSchema(modelName(contentType, "Response$code"))
            }
        }

    fun allSchemas() = requestSchemas() + responseSchemas()
}

fun OpenApi3Spec.flattenedPaths() = paths.entries.flatMap { (path, verbs) -> verbs.map { PathV3(path, Method.valueOf(it.key.toUpperCase()), it.value) } }

fun OpenApi3Spec.apiName() = info.title.capitalize()
