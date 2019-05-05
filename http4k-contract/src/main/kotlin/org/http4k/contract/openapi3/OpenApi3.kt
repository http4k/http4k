package org.http4k.contract.openapi3

import org.http4k.contract.ContractRenderer
import org.http4k.contract.ContractRoute
import org.http4k.contract.HttpMessageMeta
import org.http4k.contract.OpenApi
import org.http4k.contract.PathSegments
import org.http4k.contract.Security
import org.http4k.contract.SecurityRenderer
import org.http4k.contract.Tag
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpMessage
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.JsonErrorResponseRenderer
import org.http4k.format.JsonLibAutoMarshallingJson
import org.http4k.lens.Failure
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.util.JsonSchema
import org.http4k.util.JsonSchemaCreator
import org.http4k.util.JsonToJsonSchema

private typealias ApiInfo = org.http4k.contract.ApiInfo

private data class OpenApiDefinition<NODE>(
    val info: ApiInfo,
    val tags: List<Tag>,
    val paths: Map<String, Map<String, OpenApiPath<NODE>>>,
    val securityDefinitions: NODE,
    val definitions: NODE
) {
    val swagger = "2.0"
    val basePath = "/"
}

private data class OpenApiPath<NODE>(
    val summary: String,
    val description: String?,
    val tags: List<String>,
    val produces: List<String>,
    val consumes: List<String>,
    val parameters: List<OpenApiParameter>,
    val responses: Map<String, OpenApiResponse<NODE>>,
    val security: NODE,
    val operationId: String?
) {
    fun definitions() =
        (parameters + responses.values).filterIsInstance<HasSchema<NODE>>().flatMap { it.definitions() }
}

private class OpenApiResponse<NODE>(val description: String?, private val jsonSchema: JsonSchema<NODE>?) : HasSchema<NODE> {
    val schema: NODE? = jsonSchema?.node

    override fun definitions() = jsonSchema?.definitions ?: emptySet()
}

interface HasSchema<NODE> {
    fun definitions(): Set<Pair<String, NODE>>
}

private sealed class OpenApiParameter(val `in`: String, val name: String, val required: Boolean, val description: String?)

private class SchemaParameter<NODE>(meta: Meta, private val jsonSchema: JsonSchema<NODE>?) : OpenApiParameter(meta.location, meta.name, meta.required, meta.description), HasSchema<NODE> {
    val schema: NODE? = jsonSchema?.node
    override fun definitions() = jsonSchema?.definitions ?: emptySet()
}

private class PrimitiveParameter(meta: Meta) : OpenApiParameter(meta.location, meta.name, meta.required, meta.description) {
    val type = meta.paramMeta.value
}

class OpenApi3<out NODE : Any>(
    private val apiInfo: ApiInfo,
    private val json: JsonLibAutoMarshallingJson<NODE>,
    private val jsonSchemaCreator: JsonSchemaCreator<Any, NODE>,
    private val securityRenderer: SecurityRenderer<NODE> = SecurityRenderer.OpenApi(json),
    private val errorResponseRenderer: JsonErrorResponseRenderer<NODE> = JsonErrorResponseRenderer(json)
) : ContractRenderer {

    private data class PathAndMethod<NODE>(val path: String, val method: Method, val pathSpec: OpenApiPath<NODE>)

    private val lens = json.autoBody<OpenApiDefinition<NODE>>().toLens()

    override fun badRequest(failures: List<Failure>) = errorResponseRenderer.badRequest(failures)

    override fun notFound() = errorResponseRenderer.notFound()

    override fun description(contractRoot: PathSegments, security: Security, routes: List<ContractRoute>): Response {
        val allSecurities = routes.mapNotNull { it.meta.security } + security
        val paths = routes.map { it.asPath(security, contractRoot) }

        return Response(OK)
            .with(lens of OpenApiDefinition(
                apiInfo,
                routes.map(ContractRoute::tags).flatten().toSet().sortedBy { it.name },
                paths
                    .groupBy { it.path }
                    .mapValues {
                        it.value.map { pam -> pam.method.name.toLowerCase() to pam.pathSpec }.toMap()
                    }
                    .toMap(),
                allSecurities.combine(),
                json.obj(paths.flatMap { it.pathSpec.definitions() })
            ))
    }

    private fun ContractRoute.asPath(contractSecurity: Security, contractRoot: PathSegments) =
        PathAndMethod(describeFor(contractRoot), method,
            OpenApiPath(
                meta.summary,
                meta.description,
                if (tags.isEmpty()) listOf(contractRoot.toString()) else tags.map { it.name }.toSet().sorted(),
                meta.produces.map { it.value }.toSet().sorted(),
                meta.consumes.map { it.value }.toSet().sorted(),
                asOpenApiParameters(),
                meta.responses.map { it.message.status.code.toString() to it.asOpenApiResponse() }.toMap(),
                securityRenderer.ref(meta.security ?: contractSecurity),
                meta.operationId
            )
        )

    private fun ContractRoute.asOpenApiParameters(): List<OpenApiParameter> {
        val jsonRequest = meta.request?.let { if (CONTENT_TYPE(it.message) == APPLICATION_JSON) it else null }

        val bodyParamNodes = meta.body?.metas?.map {
            when (it.paramMeta) {
                ObjectParam -> SchemaParameter(it, jsonRequest?.toSchema())
                else -> PrimitiveParameter(it)
            }
        } ?: emptyList()

        val nonBodyParamNodes = nonBodyParams.map {
            when (it.paramMeta) {
                ObjectParam -> SchemaParameter<NODE>(it, null)
                else -> PrimitiveParameter(it)
            }
        }
        return nonBodyParamNodes + bodyParamNodes
    }

    private fun HttpMessageMeta<Response>.asOpenApiResponse() = OpenApiResponse(description, toSchema())

    private fun HttpMessageMeta<HttpMessage>.toSchema() = example
        ?.let { jsonSchemaCreator.toSchema(it, definitionId) }
        ?: JsonToJsonSchema(json).toSchema(json.parse(message.bodyString()))

    private fun List<Security>.combine() = json { obj(flatMap { fields(securityRenderer.full(it)) }) }

    companion object
}
