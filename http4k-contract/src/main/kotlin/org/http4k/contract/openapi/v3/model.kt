package org.http4k.contract.openapi.v3

import org.http4k.contract.Tag
import org.http4k.contract.openapi.ApiInfo
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta
import org.http4k.util.JsonSchema

data class Api<NODE>(
    val info: ApiInfo,
    val tags: List<Tag>,
    val paths: Map<String, Map<String, ApiPath<NODE>>>,
    val components: Components<NODE>
) {
    val openapi = "3.0.0"
}

data class Components<NODE>(
    val schemas: NODE,
    val securitySchemes: NODE
)

data class ApiPath<NODE>(
    val summary: String,
    val description: String?,
    val tags: List<String>?,
    val parameters: List<RequestParameter>?,
    val requestBody: RequestContents<NODE>?,
    val responses: Map<String, ResponseContents<NODE>>,
    val security: NODE?,
    val operationId: String?
) {
    fun definitions() = listOfNotNull(
        responses.flatMap { it.value.definitions() },
        parameters?.filterIsInstance<HasSchema<NODE>>()?.flatMap { it.definitions() },
        requestBody?.definitions()?.toList()
    ).flatten()
}

interface HasSchema<NODE> {
    fun definitions(): Iterable<Pair<String, NODE>>
}

sealed class BodyContent {
    class NoSchema(paramMeta: ParamMeta) : BodyContent() {
        val schema = mapOf("type" to paramMeta.value)
    }

    class SchemaContent<NODE>(private val jsonSchema: JsonSchema<NODE>?, val example: NODE?) : BodyContent(), HasSchema<NODE> {
        val schema = jsonSchema?.node
        override fun definitions() = jsonSchema?.definitions ?: emptySet()
    }

    class FormContent(val schema: FormSchema) : BodyContent() {
        class FormSchema(metas: List<Meta>) {
            val type = "object"
            val properties = metas.map { it.name to mapOf("type" to it.paramMeta.value, "description" to it.description) }.toMap()
            val required = metas.filter(Meta::required).map { it.name }
        }
    }
}

class RequestContents<NODE>(val content: Map<String, BodyContent>?) : HasSchema<NODE> {
    override fun definitions() = content?.values
        ?.filterIsInstance<HasSchema<NODE>>()
        ?.flatMap { it.definitions() } ?: emptySet<Pair<String, NODE>>()

    val required = content != null
}

class ResponseContents<NODE>(val description: String?, val content: Map<String, BodyContent>) : HasSchema<NODE> {
    override fun definitions() = content.values
        .filterIsInstance<HasSchema<NODE>>()
        .flatMap { it.definitions() }.toSet()
}

sealed class RequestParameter(val `in`: String, val name: String, val required: Boolean, val description: String?) {
    class SchemaParameter<NODE>(meta: Meta, private val jsonSchema: JsonSchema<NODE>?) : RequestParameter(meta.location, meta.name, meta.required, meta.description), HasSchema<NODE> {
        val schema: NODE? = jsonSchema?.node
        override fun definitions() = jsonSchema?.definitions ?: emptySet()
    }

    class PrimitiveParameter<NODE>(meta: Meta, val schema: NODE) : RequestParameter(meta.location, meta.name, meta.required, meta.description)
}