package org.http4k.openapi.v3

import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName
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
                messageSpec.schema?.namedSchema(modelName(contentType, "Request"))
            }
        ).flatten()

    fun responseSchemas(): List<NamedSchema> = pathSpec.responses.entries
        .flatMap { (code, messageSpec) ->
            messageSpec.content.entries.mapNotNull { (contentType, messageSpec) ->
                messageSpec.schema?.namedSchema(modelName(contentType, "Response$code"))
            }
        }

    fun allSchemas() = requestSchemas() + responseSchemas()
}

sealed class NamedSchema(name: String) {
    val fieldName = name.clean().decapitalize()

    data class Existing(val name: String, val typeName: TypeName) : NamedSchema(name)
    data class Generated(val name: String, val schema: SchemaSpec) : NamedSchema(name)
}

fun String.clean() = filter { it.isLetterOrDigit() }

fun OpenApi3Spec.flattenedPaths() = paths.entries.flatMap { (path, verbs) -> verbs.map { Path(path, Method.valueOf(it.key.toUpperCase()), it.value) } }

fun OpenApi3Spec.apiName() = info.title.capitalize()

private fun SchemaSpec.namedSchema(modelName: String): NamedSchema = when (this) {
    is SchemaSpec.RefSpec -> NamedSchema.Generated(schemaName, this)
    is SchemaSpec.ArraySpec -> when (val itemSchema = itemsSpec().namedSchema(modelName)) {
        is NamedSchema.Generated -> itemSchema
        is NamedSchema.Existing -> NamedSchema.Existing(modelName, List::class.asTypeName().parameterizedBy(itemSchema.typeName))
    }
    else -> clazz?.let { NamedSchema.Existing(modelName, it.asClassName()) }
        ?: NamedSchema.Generated(modelName, this)
}
