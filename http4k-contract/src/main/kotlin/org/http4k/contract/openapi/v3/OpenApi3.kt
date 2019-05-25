package org.http4k.contract.openapi.v3

import org.http4k.contract.ContractRenderer
import org.http4k.contract.ContractRoute
import org.http4k.contract.ErrorResponseRenderer
import org.http4k.contract.HttpMessageMeta
import org.http4k.contract.JsonErrorResponseRenderer
import org.http4k.contract.PathSegments
import org.http4k.contract.RouteMeta
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.contract.openapi.Render
import org.http4k.contract.openapi.SecurityRenderer
import org.http4k.contract.openapi.v2.JsonToJsonSchema
import org.http4k.contract.openapi.v3.BodyContent.FormContent
import org.http4k.contract.openapi.v3.BodyContent.FormContent.FormSchema
import org.http4k.contract.openapi.v3.BodyContent.NoSchema
import org.http4k.contract.openapi.v3.BodyContent.SchemaContent
import org.http4k.contract.openapi.v3.RequestParameter.PrimitiveParameter
import org.http4k.contract.openapi.v3.RequestParameter.SchemaParameter
import org.http4k.contract.security.Security
import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.HttpMessage
import org.http4k.core.Method
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.HEAD
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Json
import org.http4k.format.JsonLibAutoMarshallingJson
import org.http4k.lens.Header.CONTENT_TYPE
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.util.JsonSchema

/**
 * Contract renderer for OpenApi3 format JSON. For the JSON schema generation, naming of
 * object models will default to either reflective or hashcode based depending on if a Auto Json
 * is passed.
 */
class OpenApi3<NODE : Any>(
    private val apiInfo: ApiInfo,
    private val json: Json<NODE>,
    private val apiRenderer: ApiRenderer<Api<NODE>, NODE> = OpenApi3ApiRenderer(json),
    private val securityRenderer: SecurityRenderer = OpenApi3SecurityRenderer,
    private val errorResponseRenderer: ErrorResponseRenderer = JsonErrorResponseRenderer(json)
) : ContractRenderer, ErrorResponseRenderer by errorResponseRenderer {
    private data class PathAndMethod<NODE>(val path: String, val method: Method, val pathSpec: ApiPath<NODE>)

    constructor(apiInfo: ApiInfo, json: JsonLibAutoMarshallingJson<NODE>) : this(apiInfo, json, ApiRenderer.Auto(json))

    override fun description(contractRoot: PathSegments, security: Security, routes: List<ContractRoute>): Response {
        val allSecurities = routes.map { it.meta.security } + security
        val paths = routes.map { it.asPath(security, contractRoot) }

        return Response(OK)
            .with(json.body().toLens() of apiRenderer.api(
                Api(
                    apiInfo,
                    routes.map(ContractRoute::tags).flatten().toSet().sortedBy { it.name },
                    paths
                        .groupBy { it.path }
                        .mapValues {
                            it.value.map { pam -> pam.method.name.toLowerCase() to pam.pathSpec }.toMap().toSortedMap()
                        }
                        .toSortedMap(),
                    Components(json.obj(paths.flatMap { it.pathSpec.definitions() }), json(allSecurities.combineFull()))
                )
            ))
    }

    private fun ContractRoute.asPath(contractSecurity: Security, contractRoot: PathSegments) =
        PathAndMethod(describeFor(contractRoot), method,
            ApiPath(
                meta.summary,
                meta.description,
                if (tags.isEmpty()) listOf(contractRoot.toString()) else tags.map { it.name }.toSet().sorted(),
                asOpenApiParameters(),
                when (method) {
                    in setOf(GET, DELETE, HEAD) -> null
                    else -> meta.requestBody()?.takeIf { it.required }
                },
                meta.responses(),
                json(listOf(meta.security, contractSecurity).combineRef()),
                meta.operationId ?: method.name.toLowerCase() + describeFor(contractRoot)
                    .split('/').joinToString("") { it.capitalize() }
            )
        )

    private fun RouteMeta.responses() =
        responses.map { it.message.status.code.toString() to it.asOpenApiResponse() }.toMap()

    private fun ContractRoute.asOpenApiParameters() = nonBodyParams.map {
        when (it.paramMeta) {
            ObjectParam -> SchemaParameter(it, "{}".toSchema())
            else -> PrimitiveParameter(it, json {
                obj("type" to string(it.paramMeta.value))
            })
        }
    }

    private fun RouteMeta.requestBody(): RequestContents<NODE>? {
        val noSchema = consumes.map { it.value to NoSchema(ParamMeta.StringParam) }

        val withSchema = requests.mapNotNull {
            when (CONTENT_TYPE(it.message)) {
                APPLICATION_JSON -> APPLICATION_JSON.value to it.toSchemaContent()
                APPLICATION_FORM_URLENCODED -> {
                    APPLICATION_FORM_URLENCODED.value to
                        (body?.metas?.let { FormContent(FormSchema(it)) } ?: SchemaContent("".toSchema(), null))
                }
                else -> null
            }
        }

        return (noSchema + withSchema)
            .takeIf { it.isNotEmpty() }
            ?.let { RequestContents(it.toMap()) }
    }

    private fun HttpMessageMeta<HttpMessage>.toSchemaContent(): SchemaContent<NODE> {
        fun exampleSchemaIsValid(schema: JsonSchema<NODE>) =
            if (example is Array<*>
                || example is Iterable<*>) !json.fields(schema.node).toMap().containsKey("\$ref")
            else true

        val jsonSchema = example?.let { apiRenderer.toSchema(it, definitionId) }
            ?.takeIf(::exampleSchemaIsValid)
            ?: message.bodyString().toSchema(definitionId)
        return SchemaContent(jsonSchema, message.bodyString().safeParse())
    }

    private fun HttpMessageMeta<Response>.asOpenApiResponse(): ResponseContents<NODE> {
        val contentTypes = CONTENT_TYPE(message)
            ?.takeIf { it == APPLICATION_JSON }
            ?.let { mapOf(it.value to toSchemaContent()) }
            ?: emptyMap()
        return ResponseContents(description, contentTypes)
    }

    private fun String.toSchema(definitionId: String? = null) = safeParse()
        ?.let { JsonToJsonSchema(json, "components/schemas").toSchema(it, definitionId) }
        ?: JsonSchema(json.obj(), emptySet())

    private fun List<Security>.combineFull(): Render<NODE> = {
        obj(mapNotNull { securityRenderer.full<NODE>(it) }.flatMap { fields(this(it)) })
    }

    private fun List<Security>.combineRef(): Render<NODE> = {
        array(mapNotNull { securityRenderer.ref<NODE>(it) }.map { this(it) })
    }

    private fun String.safeParse(): NODE? = try {
        json.parse(this)
    } catch (e: Exception) {
        null
    }

    companion object
}