package org.http4k.contract.openapi.v2

import org.http4k.contract.ContractRenderer
import org.http4k.contract.ContractRoute
import org.http4k.contract.ErrorResponseRenderer
import org.http4k.contract.HttpMessageMeta
import org.http4k.contract.JsonErrorResponseRenderer
import org.http4k.contract.PathSegments
import org.http4k.contract.ResponseMeta
import org.http4k.contract.Tag
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.SecurityRenderer
import org.http4k.contract.security.Security
import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Json
import org.http4k.lens.Failure
import org.http4k.lens.Header
import org.http4k.lens.Meta
import org.http4k.lens.ParamMeta
import org.http4k.util.JsonSchema
import org.http4k.util.JsonSchemaCreator
import org.http4k.util.JsonToJsonSchema

/**
 * Contract renderer for OpenApi2 format JSON. Note that for the JSON schema generation, auto-naming of
 * object models is used as the input relies on JSON objects and not JVM classees.
 */
open class OpenApi2<out NODE>(
    private val apiInfo: ApiInfo,
    private val json: Json<NODE>,
    private val securityRenderer: SecurityRenderer = OpenApi2SecurityRenderer,
    private val schemaGenerator: JsonSchemaCreator<NODE, NODE> = JsonToJsonSchema(json),
    private val errorResponseRenderer: ErrorResponseRenderer = JsonErrorResponseRenderer(json)
) : ContractRenderer, ErrorResponseRenderer by JsonErrorResponseRenderer(json) {
    override fun badRequest(failures: List<Failure>) = errorResponseRenderer.badRequest(failures)

    override fun notFound() = errorResponseRenderer.notFound()

    override fun description(contractRoot: PathSegments, security: Security, routes: List<ContractRoute>) =
        with(renderPaths(routes, contractRoot, security)) {
            Response(Status.OK)
                .with(Header.CONTENT_TYPE of ContentType.APPLICATION_JSON)
                .body(json {
                    pretty(obj(
                        "swagger" to string("2.0"),
                        "info" to apiInfo.asJson(),
                        "basePath" to string("/"),
                        "tags" to array(renderTags(routes)),
                        "paths" to obj(fields.sortedBy { it.first }),
                        "securityDefinitions" to (listOf(security) + routes.map { it.meta.security }).combine(),
                        "definitions" to obj(definitions)
                    ))
                })
        }

    private fun List<Security>.combine() =
        json { obj(mapNotNull { securityRenderer.full<NODE>(it) }.flatMap { fields(this(it)) }) }

    private fun renderPaths(routes: List<ContractRoute>, contractRoot: PathSegments, security: Security): FieldsAndDefinitions<NODE> = routes
        .groupBy { it.describeFor(contractRoot) }.entries
        .fold(FieldsAndDefinitions()) { memo, (path, routes) ->
            val routeFieldsAndDefinitions = routes.fold(FieldsAndDefinitions<NODE>()) { memoFields, route ->
                memoFields + render(contractRoot, security, route)
            }
            memo + FieldAndDefinitions(
                path to json { obj(routeFieldsAndDefinitions.fields) }, routeFieldsAndDefinitions.definitions
            )
        }

    private fun renderMeta(meta: Meta, schema: JsonSchema<NODE>? = null) = json {
        obj(
            listOf(
                "in" to string(meta.location),
                "name" to string(meta.name),
                "required" to boolean(meta.required),
                when (ParamMeta.ObjectParam) {
                    meta.paramMeta -> "schema" to (schema?.node ?: obj("type" to string(meta.paramMeta.value)))
                    else -> "type" to string(meta.paramMeta.value)
                }
            ) + (meta.description?.let { listOf("description" to string(it)) } ?: emptyList())
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
                } else "type" to string(paramMeta.value)
            ) + (description?.let { listOf("description" to string(it)) } ?: emptyList())
        )
    }

    private fun render(pathSegments: PathSegments, contractSecurity: Security, route: ContractRoute)
        : FieldAndDefinitions<NODE> {
        val (responses, responseDefinitions) = render(route.meta.responses)

        val schema = route.meta.requests.find { Header.CONTENT_TYPE(it.message) == ContentType.APPLICATION_JSON }?.asSchema()

        val bodyParamNodes = route.spec.routeMeta.body?.metas?.map { it.renderBodyMeta(schema) } ?: emptyList()

        val nonBodyParamNodes = route.nonBodyParams.flatMap { it.asList() }.map { renderMeta(it) }

        val routeTags = if (route.tags.isEmpty()) listOf(json.string(pathSegments.toString())) else route.tagNames()
        val consumes = route.meta.consumes + (route.spec.routeMeta.body?.let { listOf(it.contentType) }
            ?: emptyList())

        return json {
            val fields =
                listOfNotNull(
                    "tags" to array(routeTags),
                    "summary" to string(route.meta.summary),
                    route.meta.operationId?.let { "operationId" to string(it) },
                    "produces" to array(route.meta.produces.map { string(it.value) }),
                    "consumes" to array(consumes.map { string(it.value) }),
                    "parameters" to array(nonBodyParamNodes + bodyParamNodes),
                    "responses" to obj(responses),
                    "security" to array(
                        listOf(route.meta.security, contractSecurity).mapNotNull {

                            securityRenderer.ref<NODE>(it)
                        }.map { json(it) })
                ) + (route.meta.description?.let { listOf("description" to string(it)) } ?: emptyList())

            FieldAndDefinitions(
                route.method.toString().toLowerCase() to obj(fields),
                ((route.meta.requests.flatMap { it.asSchema().definitions }) + responseDefinitions).toSet())
        }
    }

    private fun HttpMessageMeta<*>.asSchema(): JsonSchema<NODE> = try {
        schemaGenerator.toSchema(json.parse(message.bodyString()), definitionId)
    } catch (e: Exception) {
        JsonSchema(json.nullNode(), emptySet())
    }

    private fun render(responses: List<HttpMessageMeta<Response>>) = json {
        (responses.takeIf { it.isNotEmpty() } ?: listOf(
            ResponseMeta(OK.description, Response(OK))
        )).fold(FieldsAndDefinitions<NODE>()) { memo, meta ->
            val (node, definitions) = meta.asSchema()

            memo + FieldAndDefinitions(
                meta.message.status.code.toString() to obj(
                    listOf("description" to string(meta.description)) +
                        if (node == nullNode()) emptyList() else listOf("schema" to node)),
                definitions)
        }
    }

    private fun ContractRoute.tagNames() = tags.map(Tag::name).map(json::string)

    private fun ApiInfo.asJson() = json {
        obj("title" to string(title), "version" to string(version), "description" to string(description ?: ""))
    }

    private fun renderTags(routes: List<ContractRoute>) = routes
        .flatMap(ContractRoute::tags).toSet()
        .sortedBy { it.name }
        .map {
            json {
                obj(listOf("name" to string(it.name)) + it.description?.let { "description" to string(it) }.asList())
            }
        }
}

private data class FieldsAndDefinitions<NODE>(val fields: List<Pair<String, NODE>> = emptyList(), val definitions: Set<Pair<String, NODE>> = emptySet()) {
    operator fun plus(fieldAndDefinitions: FieldAndDefinitions<NODE>) = FieldsAndDefinitions(fields + fieldAndDefinitions.field,
        fieldAndDefinitions.definitions + definitions)
}

private data class FieldAndDefinitions<out NODE>(val field: Pair<String, NODE>, val definitions: Set<Pair<String, NODE>>)

private fun <T> T?.asList() = this?.let(::listOf) ?: listOf()