package org.http4k.contract

import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.JsonErrorResponseRenderer
import org.http4k.format.JsonLibAutoMarshallingJson
import org.http4k.lens.Failure
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.util.JsonSchemaCreator
import org.http4k.util.JsonToJsonSchema

private data class OpenApiDefinition<NODE>(
    val apiInfo: ApiInfo,
    val tags: List<Tag>,
    val securityDefinitions: NODE,
    val paths: List<OpenApiPath<NODE>>
) {
    val swagger = "2.0"
    val basePath = "/"
}

private data class OpenApiPath<NODE>(
    val summary: String,
    val tags: List<Tag>,
    val produces: List<String>,
    val consumes: List<String>,
    val parameters: List<OpenApiParameter>,
    val responses: Map<String, OpenApiResponse<NODE>>,
    val security: NODE,
    val operationId: String?
)

private data class OpenApiResponse<NODE>(val description: String?, val schema: NODE?)

private sealed class OpenApiParameter(val `in`: String, val name: String, val required: Boolean, val description: String?)

private class SchemaParameter<NODE>(meta: Meta, val schema: NODE) : OpenApiParameter(meta.location, meta.name, meta.required, meta.description)

private class PrimitiveParameter(meta: Meta) : OpenApiParameter(meta.location, meta.name, meta.required, meta.description)

open class AutoOpenApi<out NODE : Any>(
    private val apiInfo: ApiInfo,
    private val json: JsonLibAutoMarshallingJson<NODE>,
    private val jsonSchemaCreator: JsonSchemaCreator<*, NODE> = JsonToJsonSchema(json),
    private val securityRenderer: SecurityRenderer<NODE> = SecurityRenderer.OpenApi(json)
) : ContractRenderer {
    private val lens = json.autoBody<OpenApiDefinition<NODE>>().toLens()

    override fun badRequest(failures: List<Failure>) = JsonErrorResponseRenderer(json).badRequest(failures)

    override fun notFound() = JsonErrorResponseRenderer(json).notFound()

    override fun description(contractRoot: PathSegments, security: Security, routes: List<ContractRoute>) =
        Response(OK)
            .with(lens of OpenApiDefinition(
                apiInfo,
                routes.renderTags(),
                securityRenderer.full(security),
                routes.map { it.asPath(security) }
            ))

    private fun ContractRoute.asPath(contractSecurity: Security) = OpenApiPath(
        meta.summary,
        tags.toSet().sortedBy { it.name },
        meta.produces.map { it.value }.toSet().sorted(),
        meta.consumes.map { it.value }.toSet().sorted(),
        asOpenApiParameters(),
        meta.responses.map { it.message.status.code.toString() to it.asOpenApiResponse() }.toMap(),
        securityRenderer.ref(meta.security ?: contractSecurity),
        meta.operationId
    )

    private fun ContractRoute.asOpenApiParameters(): List<OpenApiParameter> {
        val bodyParamNodes = meta.body?.metas?.map { it.asBodyOpenApiParameter() } ?: emptyList()
        val nonBodyParamNodes = nonBodyParams.flatMap { it.asList() }.map { it.asOpenApiParameter() }
        return nonBodyParamNodes + bodyParamNodes
    }

    private fun Meta.asBodyOpenApiParameter() = when (paramMeta) {
        ObjectParam -> SchemaParameter(this, null)
        else -> PrimitiveParameter(this)
    }

    private fun Meta.asOpenApiParameter() = when (paramMeta) {
        ObjectParam -> SchemaParameter(this, null)
        else -> PrimitiveParameter(this)
    }

    private fun HttpMessageMeta<Response>.asOpenApiResponse() = OpenApiResponse<NODE>(description, null)

    private fun List<ContractRoute>.renderTags() = flatMap(ContractRoute::tags).toSet().sortedBy { it.name }
}

private fun <T> T?.asList() = this?.let(::listOf) ?: listOf()
