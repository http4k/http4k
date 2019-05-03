package org.http4k.contract

import org.http4k.core.ContentType.Companion.APPLICATION_JSON
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
import org.http4k.util.JsonSchemaCreator
import org.http4k.util.JsonToJsonSchema

data class ApiInfo(val title: String, val version: String, val description: String? = null)

open class OpenApi<out NODE>(
    private val apiInfo: ApiInfo,
    private val json: Json<NODE>,
    private val securityRenderer: SecurityRenderer<NODE> = SecurityRenderer.OpenApi(json),
    private val schemaGenerator: JsonSchemaCreator<NODE, NODE> = JsonToJsonSchema(json),
    private val errorResponseRenderer: JsonErrorResponseRenderer<NODE> = JsonErrorResponseRenderer(json)
) : ContractRenderer {
    override fun badRequest(failures: List<Failure>) = errorResponseRenderer.badRequest(failures)

    override fun notFound() = errorResponseRenderer.notFound()

    override fun description(contractRoot: PathSegments, security: Security, routes: List<ContractRoute>) =
        with(renderPaths(routes, contractRoot, security)) {
            Response(OK)
                .with(CONTENT_TYPE of APPLICATION_JSON)
                .body(json {
                    pretty(obj(
                        "swagger" to string("2.0"),
                        "info" to apiInfo.asJson(),
                        "basePath" to string("/"),
                        "tags" to array(renderTags(routes)),
                        "paths" to obj(fields),
                        "securityDefinitions" to securityRenderer.full(security),
                        "definitions" to obj(definitions)
                    ))
                })
        }

    private fun renderPaths(routes: List<ContractRoute>, contractRoot: PathSegments, security: Security): FieldsAndDefinitions<NODE> = routes
        .groupBy { it.describeFor(contractRoot) }.entries
        .fold(FieldsAndDefinitions()) { memo, (path, routes) ->
            val routeFieldsAndDefinitions = routes.fold(FieldsAndDefinitions<NODE>()) { memoFields, route ->
                memoFields.add(render(contractRoot, security, route))
            }
            memo.add(path to json { obj(routeFieldsAndDefinitions.fields) }, routeFieldsAndDefinitions.definitions)
        }

    private fun renderMeta(meta: Meta, schema: JsonSchema<NODE>? = null): NODE = json {
        obj(
            listOf(
                "in" to string(meta.location),
                "name" to string(meta.name),
                "required" to boolean(meta.required),
                when (ObjectParam) {
                    meta.paramMeta -> "schema" to (schema?.node ?: obj("type" to string(meta.paramMeta.value)))
                    else -> "type" to string(meta.paramMeta.value)
                }
            ) + (meta.description?.let { listOf("description" to string(it)) } ?: emptyList())
        )
    }

    private fun renderBodyMeta(meta: Meta, schema: JsonSchema<NODE>? = null): NODE = json {
        obj(
            listOf(
                "in" to string(meta.location),
                "name" to string(meta.name),
                "required" to boolean(meta.required),
                if (meta.location != "formData") {
                    "schema" to (schema?.node ?: obj("type" to string(meta.paramMeta.value)))
                } else "type" to string(meta.paramMeta.value)
            ) + (meta.description?.let { listOf("description" to string(it)) } ?: emptyList())
        )
    }

    private fun render(pathSegments: PathSegments, contractSecurity: Security, route: ContractRoute): FieldAndDefinitions<NODE> {
        val (responses, responseDefinitions) = render(route.meta.responses)

        val schema = route.jsonRequest?.asSchema()

        val bodyParamNodes = route.spec.routeMeta.body?.metas?.map { renderBodyMeta(it, schema) } ?: emptyList()

        val nonBodyParamNodes = route.nonBodyParams.flatMap { it.asList() }.map { renderMeta(it) }

        val routeTags = if (route.tags.isEmpty()) listOf(json.string(pathSegments.toString())) else route.tagNames()
        val consumes = route.meta.consumes + (route.spec.routeMeta.body?.let { listOf(it.contentType) }
            ?: emptyList())

        return json {
            val fields =
                listOf(
                    "tags" to array(routeTags),
                    "summary" to string(route.meta.summary),
                    route.meta.operationId?.let { "operationId" to string(it) },
                    "produces" to array(route.meta.produces.map { string(it.value) }),
                    "consumes" to array(consumes.map { string(it.value) }),
                    "parameters" to array(nonBodyParamNodes + bodyParamNodes),
                    "responses" to obj(responses),
                    "security" to securityRenderer.ref(route.meta.security ?: contractSecurity)
                ) + (route.meta.description?.let { listOf("description" to string(it)) } ?: emptyList())

            val defs = route.meta.request.asList().flatMap { it.asSchema().definitions }.plus(responseDefinitions).toSet()
            FieldAndDefinitions(route.method.toString().toLowerCase() to
                obj(*fields.filterNotNull().toTypedArray()), defs)
        }
    }

    private fun HttpMessageMeta<*>.asSchema(): JsonSchema<NODE> = try {
        schemaGenerator.toSchema(json.parse(message.bodyString()), definitionId)
    } catch (e: Exception) {
        JsonSchema(json.nullNode(), emptySet())
    }

    private fun render(responses: List<HttpMessageMeta<Response>>) = json {
        responses.fold(FieldsAndDefinitions<NODE>()) { memo, meta ->
            val (node, definitions) = meta.asSchema()
            val newField = meta.message.status.code.toString() to obj(
                listOf("description" to string(meta.description)) +
                    if (node == nullNode()) emptyList() else listOf("schema" to node))
            memo.add(newField, definitions)
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
    fun add(newField: Pair<String, NODE>, newDefinitions: Set<Pair<String, NODE>>) = FieldsAndDefinitions(fields + newField, newDefinitions + definitions)

    fun add(fieldAndDefinitions: FieldAndDefinitions<NODE>) = FieldsAndDefinitions(fields + fieldAndDefinitions.field,
        fieldAndDefinitions.definitions + definitions)
}

private data class FieldAndDefinitions<out NODE>(val field: Pair<String, NODE>, val definitions: Set<Pair<String, NODE>>)

private fun <T> T?.asList() = this?.let(::listOf) ?: listOf()
