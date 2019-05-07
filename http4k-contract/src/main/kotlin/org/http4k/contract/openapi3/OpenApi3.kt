package org.http4k.contract.openapi3

import org.http4k.contract.ContractRenderer
import org.http4k.contract.ContractRoute
import org.http4k.contract.HttpMessageMeta
import org.http4k.contract.PathSegments
import org.http4k.contract.RouteMeta
import org.http4k.contract.Security
import org.http4k.contract.SecurityRenderer
import org.http4k.contract.Tag
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpMessage
import org.http4k.core.Method
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.HEAD
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

private data class Api<NODE>(
    val info: ApiInfo,
    val tags: List<Tag>,
    val paths: Map<String, Map<String, Path<NODE>>>,
    val components: Components<NODE>
) {
    val openapi = "3.0.0"
}

private data class Components<NODE>(
    val schemas: NODE,
    val securitySchemes: NODE
)

private data class Path<NODE>(
    val summary: String,
    val description: String?,
    val tags: List<String>?,
    val parameters: List<Parameter>?,
    val requestBody: RequestContents<NODE>?,
    val responses: Map<String, ResponseContents<NODE>>,
    val security: NODE,
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

private class MessageContent<NODE>(private val jsonSchema: JsonSchema<NODE>?) : HasSchema<NODE> {
    val schema: NODE? = jsonSchema?.node

    override fun definitions() = jsonSchema?.definitions ?: emptySet()
}

private class RequestContents<NODE>(val content: Map<String, MessageContent<NODE>>?) : HasSchema<NODE> {
    override fun definitions() = content?.values?.flatMap { it.definitions() } ?: emptySet<Pair<String, NODE>>()

    val required = content != null
}

private class ResponseContents<NODE>(val description: String?, val content: Map<String, MessageContent<NODE>>) : HasSchema<NODE> {
    override fun definitions() = content.values.flatMap { it.definitions() }.toSet()
}

private sealed class Parameter(val `in`: String, val name: String, val required: Boolean, val description: String?)

private class SchemaParameter<NODE>(meta: Meta, private val jsonSchema: JsonSchema<NODE>?) : Parameter(meta.location, meta.name, meta.required, meta.description), HasSchema<NODE> {
    val schema: NODE? = jsonSchema?.node
    override fun definitions() = jsonSchema?.definitions ?: emptySet()
}

private class PrimitiveParameter(meta: Meta) : Parameter(meta.location, meta.name, meta.required, meta.description) {
    val type = meta.paramMeta.value
}

class OpenApi3<out NODE : Any>(
    private val apiInfo: ApiInfo,
    private val json: JsonLibAutoMarshallingJson<NODE>,
    private val jsonSchemaCreator: JsonSchemaCreator<Any, NODE>,
    private val securityRenderer: SecurityRenderer = OpenApi3SecurityRenderer,
    private val errorResponseRenderer: JsonErrorResponseRenderer<NODE> = JsonErrorResponseRenderer(json)
) : ContractRenderer {

    private data class PathAndMethod<NODE>(val path: String, val method: Method, val pathSpec: Path<NODE>)

    private val lens = json.autoBody<Api<NODE>>().toLens()

    override fun badRequest(failures: List<Failure>) = errorResponseRenderer.badRequest(failures)

    override fun notFound() = errorResponseRenderer.notFound()

    override fun description(contractRoot: PathSegments, security: Security, routes: List<ContractRoute>): Response {
        val allSecurities = routes.mapNotNull { it.meta.security } + security
        val paths = routes.map { it.asPath(security, contractRoot) }

        return Response(OK)
            .with(lens of Api(
                apiInfo,
                routes.map(ContractRoute::tags).flatten().toSet().sortedBy { it.name },
                paths
                    .groupBy { it.path }
                    .mapValues {
                        it.value.map { pam -> pam.method.name.toLowerCase() to pam.pathSpec }.toMap().toSortedMap()
                    }
                    .toSortedMap(),
                Components(json.obj(paths.flatMap { it.pathSpec.definitions() }), allSecurities.combine())
            ))
    }

    private fun ContractRoute.asPath(contractSecurity: Security, contractRoot: PathSegments) =
        PathAndMethod(describeFor(contractRoot), method,
            Path(
                meta.summary,
                meta.description,
                if (tags.isEmpty()) listOf(contractRoot.toString()) else tags.map { it.name }.toSet().sorted().nullIfEmpty(),
                asOpenApiParameters().nullIfEmpty(),
                when (method) {
                    in setOf(GET, DELETE, HEAD) -> null
                    else -> meta.requestBody().takeIf { it.required }
                },
                meta.responses(),
                json(securityRenderer.ref(meta.security ?: contractSecurity)),
                meta.operationId
            )
        )

    private fun RouteMeta.responses() =
        responses.map { it.message.status.code.toString() to it.asOpenApiResponse() }.toMap()

    private fun ContractRoute.asOpenApiParameters() = nonBodyParams.map {
        when (it.paramMeta) {
            ObjectParam -> SchemaParameter<NODE>(it, null)
            else -> PrimitiveParameter(it)
        }
    }

    private fun RouteMeta.requestBody(): RequestContents<NODE> {
        val noSchema = consumes.map { it.value to MessageContent<NODE>(null) }
        val withSchema = requests.mapNotNull {
            when (CONTENT_TYPE(it.message)) {
                APPLICATION_JSON -> APPLICATION_JSON.value to MessageContent(it.toSchema())
                else -> null
            }
        }

        return RequestContents((noSchema + withSchema).nullIfEmpty()?.toMap())
    }

    private fun HttpMessageMeta<Response>.asOpenApiResponse(): ResponseContents<NODE> {
        val contentTypes = CONTENT_TYPE(message)
            ?.takeIf { it == APPLICATION_JSON }
            ?.let { mapOf(it.value to MessageContent(this.toSchema())) }
        return ResponseContents(description, contentTypes ?: emptyMap())
    }

    private fun HttpMessageMeta<HttpMessage>.toSchema(): JsonSchema<NODE> = example
        ?.let { jsonSchemaCreator.toSchema(it, definitionId) }
        ?: JsonToJsonSchema(json).toSchema(json.parse(message.bodyString()))

    private fun List<Security>.combine() = json { obj(flatMap { fields(json(securityRenderer.full(it))) }) }

    companion object
}

private fun <E : Iterable<T>, T> E.nullIfEmpty(): E? = if (iterator().hasNext()) this else null