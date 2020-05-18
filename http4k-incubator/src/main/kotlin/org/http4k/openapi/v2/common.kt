package org.http4k.openapi.v2

import org.http4k.core.ContentType
import org.http4k.core.Method
import org.http4k.openapi.NamedSchema
import org.http4k.openapi.namedSchema

data class PathV2(val urlPathPattern: String, val method: Method, val pathV2Spec: PathV2Spec) {
    val uniqueName = (pathV2Spec.operationId
        ?: method.toString().toLowerCase() + urlPathPattern.replace('/', '_')).capitalize()

    private fun modelName(contentType: String, suffix: String) =
        uniqueName + ContentType(contentType).value.substringAfter('/').capitalize().filter(Char::isLetterOrDigit) + suffix

    fun requestSchemas(): List<NamedSchema> =
        listOfNotNull(pathV2Spec.requestBody?.content?.entries
            ?.mapNotNull { (contentType, messageSpec) ->
                messageSpec.schema?.namedSchema(modelName(contentType, "Request"))
            }
        ).flatten()

    fun responseSchemas(): List<NamedSchema> = pathV2Spec.responses.entries
        .flatMap { (code, messageSpec) ->
            messageSpec.content.entries.mapNotNull { (contentType, messageSpec) ->
                messageSpec.schema?.namedSchema(modelName(contentType, "Response$code"))
            }
        }

    fun allSchemas() = requestSchemas() + responseSchemas()
}

fun OpenApi2Spec.flattenedPaths() = paths.entries.flatMap { (path, verbs) -> verbs.map { PathV2(path, Method.valueOf(it.key.toUpperCase()), it.value) } }

fun OpenApi2Spec.apiName() = info.title.capitalize()
