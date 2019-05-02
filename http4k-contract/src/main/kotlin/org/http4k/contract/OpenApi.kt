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
import org.http4k.util.JsonToJsonSchema

data class ApiInfo(val title: String, val version: String, val description: String? = null)

open class OpenApi<out NODE>(private val apiInfo: ApiInfo, private val json: Json<NODE>) : ContractRenderer {

    private val schemaGenerator = JsonToJsonSchema(json)

    override fun badRequest(failures: List<Failure>) = JsonErrorResponseRenderer(json).badRequest(failures)

    override fun notFound() = JsonErrorResponseRenderer(json).notFound()

    override fun description(contractRoot: PathSegments, security: Security, routes: List<ContractRoute>): Response {
        val paths = renderPaths(routes, contractRoot, security)
        return Response(OK)
            .with(CONTENT_TYPE of APPLICATION_JSON)
            .body(json {
                pretty(obj(
                    "swagger" to string("2.0"),
                    "info" to apiInfo.asJson(),
                    "basePath" to string("/"),
                    "tags" to array(renderTags(routes)),
                    "paths" to obj(paths.fields),
                    "securityDefinitions" to security.asJson(),
                    "definitions" to obj(paths.definitions)
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

    private fun renderTags(routes: List<ContractRoute>) = routes.flatMap(ContractRoute::tags).toSet().sortedBy { it.name }.map { it.asJson() }

    private fun render(pathSegments: PathSegments, contractSecurity: Security, route: ContractRoute): FieldAndDefinitions<NODE> {
        val (responses, responseDefinitions) = render(route.meta.responses)

        val schema = route.jsonRequest?.asSchema()

        val bodyParamNodes = route.spec.routeMeta.body?.metas?.map { renderBodyMeta(it, schema) } ?: emptyList()

        val nonBodyParamNodes = route.nonBodyParams.flatMap { it.asList() }.map { renderMeta(it) }

        val routeTags = if (route.tags.isEmpty()) listOf(json.string(pathSegments.toString())) else route.tagsAsJson()
        val consumes = route.meta.consumes + (route.spec.routeMeta.body?.let { listOf(it.contentType) }
            ?: emptyList())

        val routeSecurity = route.meta.security ?: contractSecurity

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
                    "security" to array(when (routeSecurity) {
                        is ApiKeySecurity<*> -> listOf(obj("api_key" to array(emptyList())))
                        is BasicAuthSecurity -> listOf(obj("basicAuth" to array(emptyList())))
                        else -> emptyList()
                    })
                ) + (route.meta.description?.let { listOf("description" to string(it)) } ?: emptyList())
            val definitions = route.meta.request.asList().flatMap { it.asSchema().definitions }.plus(responseDefinitions).toSet()

            FieldAndDefinitions(route.method.toString().toLowerCase() to
                obj(*fields.filterNotNull().toTypedArray()), definitions)
        }

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

    private fun Security.asJson() = json {
        when (this@asJson) {
            is BasicAuthSecurity -> obj(
                "basicAuth" to obj(
                    "type" to string("basic")
                )
            )
            is ApiKeySecurity<*> -> obj(
                "api_key" to obj(
                    "type" to string("apiKey"),
                    "in" to string(param.meta.location),
                    "name" to string(param.meta.name)
                ))
            else -> obj(listOf())
        }
    }

    private fun HttpMessageMeta<*>.asSchema(): JsonSchema<NODE> = try {
        schemaGenerator.toSchema(json.parse(message.bodyString()), definitionId)
    } catch (e: Exception) {
        JsonSchema(json.nullNode(), emptySet())
    }

    private fun ContractRoute.tagsAsJson() = tags.map(Tag::name).map(json::string)

    private fun ApiInfo.asJson() = json {
        obj("title" to string(title), "version" to string(version), "description" to string(description
            ?: ""))
    }

    private fun Tag.asJson() = json { obj(listOf("name" to string(name)).plus(description?.let { "description" to string(it) }.asList())) }
}

private data class FieldsAndDefinitions<NODE>(val fields: List<Pair<String, NODE>> = emptyList(), val definitions: Set<Pair<String, NODE>> = emptySet()) {
    fun add(newField: Pair<String, NODE>, newDefinitions: Set<Pair<String, NODE>>) = FieldsAndDefinitions(fields.plus(newField), newDefinitions.plus(definitions))

    fun add(fieldAndDefinitions: FieldAndDefinitions<NODE>) = FieldsAndDefinitions(fields.plus(fieldAndDefinitions.field),
        fieldAndDefinitions.definitions.plus(definitions))
}

private data class FieldAndDefinitions<out NODE>(val field: Pair<String, NODE>, val definitions: Set<Pair<String, NODE>>)

private fun <T> T?.asList() = this?.let(::listOf) ?: listOf()
