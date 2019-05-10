package org.http4k.contract.openapi.v2

import org.http4k.contract.ContractRenderer
import org.http4k.contract.ContractRoute
import org.http4k.contract.HttpMessageMeta
import org.http4k.contract.PathSegments
import org.http4k.contract.Security
import org.http4k.contract.Tag
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.contract.openapi.SecurityRenderer
import org.http4k.contract.openapi.v2.RequestParameter.PrimitiveParameter
import org.http4k.contract.openapi.v2.RequestParameter.SchemaParameter
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpMessage
import org.http4k.core.Method
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Json
import org.http4k.format.JsonErrorResponseRenderer
import org.http4k.lens.Failure
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.util.JsonSchema
import org.http4k.util.JsonToJsonSchema

data class Api<NODE>(
    val info: ApiInfo,
    val tags: List<Tag>,
    val paths: Map<String, Map<String, ApiPath<NODE>>>,
    val securityDefinitions: NODE,
    val definitions: NODE
) {
    val swagger = "2.0"
    val basePath = "/"
}

data class ApiPath<NODE>(
    val summary: String,
    val description: String?,
    val tags: List<String>,
    val produces: List<String>,
    val consumes: List<String>,
    val parameters: List<RequestParameter>,
    val responses: Map<String, ResponseContent<NODE>>,
    val security: NODE?,
    val operationId: String?
) {
    fun definitions() =
        (parameters + responses.values)
            .filterIsInstance<HasSchema<NODE>>()
            .flatMap { it.definitions() }
            .sortedBy { it.first }
}

interface HasSchema<NODE> {
    fun definitions(): Set<Pair<String, NODE>>
}

class ResponseContent<NODE>(val description: String?, private val jsonSchema: JsonSchema<NODE>?) : HasSchema<NODE> {
    val schema: NODE? = jsonSchema?.node
    override fun definitions() = jsonSchema?.definitions ?: emptySet()
}

sealed class RequestParameter(val `in`: String, val name: String, val required: Boolean, val description: String?) {
    class SchemaParameter<NODE>(meta: Meta, private val jsonSchema: JsonSchema<NODE>?)
        : RequestParameter(meta.location, meta.name, meta.required, meta.description), HasSchema<NODE> {
        val schema: NODE? = jsonSchema?.node
        override fun definitions() = jsonSchema?.definitions ?: emptySet()
    }

    class PrimitiveParameter(meta: Meta) : RequestParameter(meta.location, meta.name, meta.required, meta.description) {
        val type = meta.paramMeta.value
    }
}

class OpenApi2<out NODE : Any>(
    private val apiInfo: ApiInfo,
    private val json: Json<NODE>,
    private val apiRenderer: ApiRenderer<Any, NODE>,
    private val securityRenderer: SecurityRenderer = org.http4k.contract.openapi.v2.SecurityRenderer,
    private val errorResponseRenderer: JsonErrorResponseRenderer<NODE> = JsonErrorResponseRenderer(json)
) : ContractRenderer {

    private data class PathAndMethod<NODE>(val path: String, val method: Method, val pathSpec: ApiPath<NODE>)

    override fun badRequest(failures: List<Failure>) = errorResponseRenderer.badRequest(failures)

    override fun notFound() = errorResponseRenderer.notFound()

    override fun description(contractRoot: PathSegments, security: Security, routes: List<ContractRoute>): Response {
        val allSecurities = routes.map { it.meta.security } + security
        val paths = routes.map { it.asPath(security, contractRoot) }

        return with(json) {
            Response(OK).with(json.body().toLens() of apiRenderer.api(Api(
                apiInfo,
                routes.map(ContractRoute::tags).flatten().toSet().sortedBy { it.name },
                paths
                    .groupBy { it.path }
                    .mapValues {
                        it.value.map { pam -> pam.method.name.toLowerCase() to pam.pathSpec }.toMap().toSortedMap()
                    }
                    .toSortedMap(),
                obj(allSecurities.mapNotNull(::full).flatMap { fields(this(it)) }),
                obj(paths.flatMap { it.pathSpec.definitions() })
            )))
        }
    }

    private fun ContractRoute.asPath(contractSecurity: Security, contractRoot: PathSegments) =
        PathAndMethod(describeFor(contractRoot), method,
            ApiPath(
                meta.summary,
                meta.description,
                if (tags.isEmpty()) listOf(contractRoot.toString()) else tags.map { it.name }.toSet().sorted(),
                meta.produces.map { it.value }.toSet().sorted(),
                meta.consumes.map { it.value }.toSet().sorted(),
                asOpenApiParameters(),
                meta.responses.map { it.message.status.code.toString() to ResponseContent(it.description, it.toSchema()) }.toMap(),
                json { array(listOf(meta.security, contractSecurity).mapNotNull(::ref).map { this(it) }) },
                meta.operationId
            )
        )

    private fun ContractRoute.asOpenApiParameters(): List<RequestParameter> {
        val jsonRequest = meta.requests.firstOrNull()?.let { if (CONTENT_TYPE(it.message) == APPLICATION_JSON) it else null }

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

    private fun HttpMessageMeta<HttpMessage>.toSchema(): JsonSchema<NODE> = example
        ?.let { apiRenderer.toSchema(it, definitionId) }
        ?: message.bodyString().toSchema(definitionId)

    private fun String.toSchema(definitionId: String? = null): JsonSchema<NODE> = try {
        JsonToJsonSchema(json).toSchema(json.parse(this), definitionId)
    } catch (e: Exception) {
        JsonSchema(json.obj(), emptySet())
    }

    private fun full(security: Security) = securityRenderer.full<NODE>(security)

    private fun ref(security: Security) = securityRenderer.ref<NODE>(security)

    companion object
}
