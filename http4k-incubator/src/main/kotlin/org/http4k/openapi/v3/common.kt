package org.http4k.openapi.v3

import com.squareup.kotlinpoet.FileSpec
import org.http4k.core.ContentType
import org.http4k.core.Method
import java.io.File

data class GenerationOptions(private val basePackage: String, val destinationFolder: File) {
    fun packageName(name: String) = "$basePackage.$name"
}

interface ApiGenerator : (OpenApi3Spec, GenerationOptions) -> List<FileSpec>

/**
 * Convenience type for working with generated code
 */
data class Path(val urlPathPattern: String, val method: Method, val pathSpec: PathSpec) {
    val uniqueName = (pathSpec.operationId
        ?: method.toString().toLowerCase() + urlPathPattern.replace('/', '_')).capitalize()

    private fun modelName(contentType: String, suffix: String) =
        uniqueName + ContentType(contentType).value.substringAfter('/').capitalize().filter(Char::isLetterOrDigit) + suffix

    fun requestSchemas(): List<NamedSchema> =
        listOfNotNull(pathSpec.requestBody?.content?.entries
            ?.mapNotNull { (contentType, messageSpec) ->
                messageSpec.schema?.let { NamedSchema(modelName(contentType, "Request"), it) }
            }
        ).flatten()

    fun responseSchemas(): List<NamedSchema> = pathSpec.responses.entries
        .flatMap { (code, messageSpec) ->
            messageSpec.content.entries.mapNotNull { (contentType, messageSpec) ->
                messageSpec.schema?.let { NamedSchema(modelName(contentType, "Response$code"), it) }
            }
        }

    fun allSchemas() = requestSchemas() + responseSchemas()
}

data class NamedSchema(val name: String, val schema: SchemaSpec)

fun OpenApi3Spec.flattenedPaths() = paths.entries.flatMap { (path, verbs) -> verbs.map { Path(path, Method.valueOf(it.key.toUpperCase()), it.value) } }

fun OpenApi3Spec.apiName() = info.title.capitalize()
