package org.http4k.contract.openapi.v2

import org.http4k.contract.ContractRenderer
import org.http4k.contract.ContractRoute
import org.http4k.contract.ErrorResponseRenderer
import org.http4k.contract.HttpMessageMeta
import org.http4k.contract.JsonErrorResponseRenderer
import org.http4k.contract.PathSegments
import org.http4k.contract.ResponseMeta
import org.http4k.contract.Tag
import org.http4k.contract.WebCallback
import org.http4k.contract.jsonschema.JsonSchema
import org.http4k.contract.jsonschema.JsonSchemaCreator
import org.http4k.contract.jsonschema.v2.JsonToJsonSchema
import org.http4k.contract.jsonschema.v2.value
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.OpenApiExtension
import org.http4k.contract.openapi.SecurityRenderer
import org.http4k.contract.security.Security
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.Json
import org.http4k.format.JsonType
import org.http4k.lens.Header
import org.http4k.lens.LensFailure
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.ArrayParam
import org.http4k.lens.ParamMeta.FileParam
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.lens.ParamMeta.StringParam
import java.io.InputStream
import java.util.Locale.getDefault

/**
 * Contract renderer for OpenApi2 format JSON. Note that for the JSON schema generation, auto-naming of
 * object models is used as the input relies on JSON objects and not JVM classees.
 */
open class OpenApi2<out NODE>(
    private val apiInfo: ApiInfo,
    private val json: Json<NODE>,
    private val baseUri: Uri? = null,
    private val extensions: List<OpenApiExtension> = emptyList(),
    private val securityRenderer: SecurityRenderer = OpenApi2SecurityRenderer,
    private val schemaGenerator: JsonSchemaCreator<NODE, NODE> = JsonToJsonSchema(json),
    private val errorResponseRenderer: ErrorResponseRenderer = JsonErrorResponseRenderer(json)
) : ContractRenderer, ErrorResponseRenderer by JsonErrorResponseRenderer(json) {
    override fun badRequest(lensFailure: LensFailure) = errorResponseRenderer.badRequest(lensFailure)

    override fun notFound() = errorResponseRenderer.notFound()

    override fun description(
        contractRoot: PathSegments,
        security: Security?,
        routes: List<ContractRoute>,
        tags: Set<Tag>,
        webhooks: Map<String, List<WebCallback>>
    ) =
        with(renderPaths(routes, contractRoot, security)) {
            Response(OK)
                .with(Header.CONTENT_TYPE of APPLICATION_JSON)
                .body(json {
                    val unextended = obj(listOfNotNull(
                        "swagger" to string("2.0"),
                        "info" to apiInfo.asJson(),
                        "basePath" to string("/"),
                        "tags" to array(routes.renderTags(tags)),
                        "paths" to obj(fields.sortedBy { it.first }),
                        "securityDefinitions" to (listOfNotNull(security) + routes.mapNotNull { it.meta.security }).combine(),
                        "definitions" to obj(definitions),
                        baseUri?.let { "host" to string(it.authority) },
                        baseUri?.let { "schemes" to array(string(it.scheme)) }
                    ))

                    pretty(extensions.fold(unextended) { acc, next -> json(next(acc)) })
                })
        }

    private fun List<Security>.combine() =
        json { obj(mapNotNull { securityRenderer.full<NODE>(it) }.flatMap { fields(this(it)) }) }

    private fun renderPaths(
        routes: List<ContractRoute>,
        contractRoot: PathSegments,
        security: Security?
    ): FieldsAndDefinitions<NODE> = routes
        .groupBy { it.describeFor(contractRoot) }.entries
        .fold(FieldsAndDefinitions()) { memo, (path, routes) ->
            val routeFieldsAndDefinitions = routes.fold(FieldsAndDefinitions<NODE>()) { memoFields, route ->
                memoFields + render(contractRoot, security, route)
            }
            memo + FieldAndDefinitions(
                normalisePath(path) to json { obj(routeFieldsAndDefinitions.fields) },
                routeFieldsAndDefinitions.definitions
            )
        }

    private fun normalisePath(path: String): String = if (path == "") "/" else path

    private fun Meta.renderMeta() = json {
        val meta = paramMeta
        obj(
            listOf(
                "in" to string(location),
                "name" to string(name),
                "required" to boolean(required)
            ) +
                when (meta) {
                    is ArrayParam -> listOf(
                        "type" to string("array"),
                        "items" to obj(
                            "type" to string(meta.itemType().coerceForSimpleType().value)
                        )
                    )

                    else -> listOf(
                        "type" to string(meta.coerceForSimpleType().value)
                    )
                } +
                (description?.let { listOf("description" to string(it)) }.orEmpty())
        )
    }

    private fun Meta.renderBodyMeta(schema: JsonSchema<NODE>? = null) = json {
        obj(
            listOf(
                "in" to string(location),
                "name" to string(name),
                "required" to boolean(required),
                if (location != "formData") {
                    "schema" to (schema?.node ?: obj("type" to string(paramMeta.value)))
                } else "type" to string(paramMeta.coerceForSimpleType().value)
            ) + (description?.let { listOf("description" to string(it)) }.orEmpty())
        )
    }

    private fun render(pathSegments: PathSegments, contractSecurity: Security?, route: ContractRoute)
        : FieldAndDefinitions<NODE> {

        val (responses, responseDefinitions) = route.meta.responses.render()

        val schema = route.meta.requests.find {
            Header.CONTENT_TYPE(it.message)?.equalsIgnoringDirectives(APPLICATION_JSON) ?: false
        }?.asSchema()

        val bodyParamNodes = route.spec.routeMeta.body?.metas?.map { it.renderBodyMeta(schema) }.orEmpty()

        val nonBodyParamNodes = route.nonBodyParams.flatMap { it.asList() }.map { it.renderMeta() }

        val routeTags = if (route.tags.isEmpty()) listOf(json.string(pathSegments.toString())) else route.tagNames()
        val consumes = route.meta.consumes + (route.spec.routeMeta.body?.let { listOf(it.contentType) }
            .orEmpty())

        return json {
            val security = listOfNotNull(route.meta.security ?: contractSecurity)
                .mapNotNull { securityRenderer.ref<NODE>(it) }.flatMap {
                    this(it).let { if (typeOf(it) == JsonType.Array) elements(it) else listOf(it) }
                }

            val fields =
                listOfNotNull(
                    "tags" to array(routeTags),
                    "summary" to string(route.meta.summary),
                    "operationId" to string(route.operationId(pathSegments)),
                    "produces" to array(route.meta.produces.map { string(it.value) }),
                    "consumes" to array(consumes.map { string(it.value) }),
                    "parameters" to array(nonBodyParamNodes + bodyParamNodes),
                    "responses" to obj(responses),
                    "security" to array(security)
                ) + (route.meta.description?.let { listOf("description" to string(it)) }.orEmpty())

            FieldAndDefinitions(
                route.method.toString().lowercase(getDefault()) to obj(fields),
                ((route.meta.requests.flatMap {
                    it.asSchema()?.definitions ?: emptyList()
                }) + responseDefinitions).toSet()
            )
        }
    }

    private fun HttpMessageMeta<*>.asSchema(): JsonSchema<NODE>? = when (example) {
        is InputStream -> null
        else -> try {
            schemaGenerator.toSchema(json.parse(message.bodyString()), definitionId, null)
        } catch (e: Exception) {
            JsonSchema(json.obj(), emptySet())
        }
    }


    private fun List<HttpMessageMeta<Response>>.render() = json {
        val all = this@render.takeIf { it.isNotEmpty() } ?: listOf(
            ResponseMeta(OK.description, Response(OK))
        )

        val collected: Map<Status, Pair<String, JsonSchema<NODE>?>> = all.groupBy { it.message.status }
            .mapValues { (_, responses) ->
                responses.first().run { description to asSchema() }
            }
        collected.entries.fold(FieldsAndDefinitions<NODE>()) { memo, entry ->
            val (status, descriptionToSchema) = entry
            val (description, schema) = descriptionToSchema

            memo + FieldAndDefinitions(
                field = status.code.toString() to obj(
                    listOf("description" to string(description)) +
                        if (schema == null) listOf("schema" to notJsonSchema(FileParam))
                        else if (schema.node == nullNode()) emptyList()
                        else listOf("schema" to schema.node)
                ),
                definitions = schema?.definitions ?: emptySet()
            )
        }
    }

    private fun notJsonSchema(fileParam: FileParam) = json {
        obj(
            "type" to string(fileParam.value),
        )
    }

    private fun ContractRoute.tagNames() = tags.map(Tag::name).map(json::string)

    private fun ApiInfo.asJson() = json {
        obj("title" to string(title), "version" to string(version), "description" to string(description.orEmpty()))
    }

    private fun List<ContractRoute>.renderTags(globalTags: Set<Tag>) =
        (flatMap(ContractRoute::tags) + globalTags).toSet()
            .sortedBy { it.name }
            .map {
                json {
                    obj(listOf("name" to string(it.name)) + it.description?.let { "description" to string(it) }
                        .asList())
                }
            }
}

private data class FieldsAndDefinitions<NODE>(
    val fields: List<Pair<String, NODE>> = emptyList(),
    val definitions: Set<Pair<String, NODE>> = emptySet()
) {
    operator fun plus(fieldAndDefinitions: FieldAndDefinitions<NODE>) = FieldsAndDefinitions(
        fields + fieldAndDefinitions.field,
        fieldAndDefinitions.definitions + definitions
    )
}

private data class FieldAndDefinitions<out NODE>(
    val field: Pair<String, NODE>,
    val definitions: Set<Pair<String, NODE>>
)

private fun <T> T?.asList() = this?.let(::listOf).orEmpty()

// we do this to continue to treat complex objects as strings in params
private fun ParamMeta.coerceForSimpleType() = when (this) {
    is ObjectParam -> StringParam
    else -> this
}

