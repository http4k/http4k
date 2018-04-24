package org.http4k.contract

import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Json
import org.http4k.format.JsonErrorResponseRenderer
import org.http4k.lens.Failure
import org.http4k.lens.Header
import org.http4k.lens.Meta
import org.http4k.util.JsonSchema
import org.http4k.util.JsonToJsonSchema

data class ApiInfo(val title: String, val version: String, val description: String? = null)

private val header = Header.optional("definitionId")

class OpenApi<ROOT : NODE, out NODE>(private val apiInfo: ApiInfo, private val json: Json<ROOT, NODE>) : ContractRenderer {

    private val schemaGenerator = JsonToJsonSchema(json)
    private val errors = JsonErrorResponseRenderer(json)

    override fun badRequest(failures: List<Failure>) = errors.badRequest(failures)

    override fun notFound() = errors.notFound()

    override fun description(contractRoot: PathSegments, security: Security, routes: List<ContractRoute>) =
        Response(OK).body(json.pretty(json.obj(
            "swagger" to json.string("2.0"),
            "info" to apiInfo.asJson(),
            "basePath" to json.string("/"),
            "tags" to json.array(renderTags(routes)),
            "paths" to json.obj(renderPaths(routes, contractRoot, security).fields),
            "securityDefinitions" to security.asJson(),
            "definitions" to json.obj(renderPaths(routes, contractRoot, security).definitions)
        )))

    private fun renderPaths(routes: List<ContractRoute>, contractRoot: PathSegments, security: Security): FieldsAndDefinitions<NODE> = routes
        .groupBy { it.describeFor(contractRoot) }.entries
        .fold(FieldsAndDefinitions(), { memo, (path, routes) ->
            val routeFieldsAndDefinitions = routes.fold(FieldsAndDefinitions<NODE>(), { memoFields, route ->
                memoFields.add(render(contractRoot, security, route))
            })
            memo.add(path to json.obj(routeFieldsAndDefinitions.fields), routeFieldsAndDefinitions.definitions)
        })

    private fun renderMeta(it: Meta, schema: JsonSchema<NODE>? = null): ROOT = json.obj(
        "in" to json.string(it.location),
        "name" to json.string(it.name),
        "description" to (it.description?.let(json::string) ?: json.nullNode()),
        "required" to json.boolean(it.required),
        schema?.let { "schema" to it.node } ?: "type" to json.string(it.paramMeta.value)
    )

    private fun renderTags(routes: List<ContractRoute>) = routes.flatMap(ContractRoute::tags).toSet().sortedBy { it.name }.map { it.asJson() }

    private fun render(pathSegments: PathSegments, security: Security, route: ContractRoute): FieldAndDefinitions<NODE> {
        val (responses, responseDefinitions) = render(route.meta.responses)

        val schema = route.jsonRequest?.asSchema()

        val bodyParamNodes = route.spec.routeMeta.body?.metas?.map { renderMeta(it, schema) } ?: emptyList()

        val nonBodyParamNodes = route.nonBodyParams.flatMap { it.asList() }.map { renderMeta(it) }

        val routeTags = if (route.tags.isEmpty()) listOf(json.string(pathSegments.toString())) else route.tagsAsJson()
        val consumes = route.meta.consumes.plus(route.spec.routeMeta.body?.let { listOf(it.contentType) } ?: emptyList())

        val fields = listOf(
            "tags" to json.array(routeTags),
            "summary" to json.string(route.meta.summary),
            "description" to (route.meta.description?.let(json::string) ?: json.nullNode()),
            route.meta.operationId?.let { "operationId" to json.string(it) },
            "produces" to json.array(route.meta.produces.map { json.string(it.value) }),
            "consumes" to json.array(consumes.map { json.string(it.value) }),
            "parameters" to json.array(nonBodyParamNodes.plus(bodyParamNodes)),
            "responses" to json.obj(responses),
            "security" to json.array(when (security) {
                is ApiKey<*> -> listOf(json.obj("api_key" to json.array(emptyList())))
                else -> emptyList<NODE>()
            })
        )

        val definitions = route.meta.request.asList().flatMap { it.asSchema().definitions }.plus(responseDefinitions).toSet()

        return FieldAndDefinitions(route.method.toString().toLowerCase() to
            json.obj(*fields.filterNotNull().toTypedArray()), definitions)
    }

    private fun render(responses: List<ResponseMeta>) =
        responses.fold(FieldsAndDefinitions<NODE>(),
            { memo, meta ->
                val (node, definitions) = meta.asSchema()
                val newField = meta.message.status.code.toString() to json.obj(
                    "description" to json.string(meta.description),
                    "schema" to node)
                memo.add(newField, definitions)
            })

    private fun Security.asJson() = when (this) {
        is ApiKey<*> -> json.obj(
            "api_key" to json.obj(
                "type" to json.string("apiKey"),
                "in" to json.string(param.meta.location),
                "name" to json.string(param.meta.name)
            ))
        else -> json.obj(listOf())
    }

    private fun HttpMessageMeta<*>.asSchema(): JsonSchema<NODE> = try {
        schemaGenerator.toSchema(json.parse(message.bodyString()), definitionId)
    } catch (e: Exception) {
        JsonSchema(json.nullNode(), emptySet())
    }

    private fun ContractRoute.tagsAsJson() = tags.map(Tag::name).map(json::string)

    private fun ApiInfo.asJson() = json.obj("title" to json.string(title), "version" to json.string(version), "description" to json.string(description ?: ""))

    private fun Tag.asJson() = json.obj(listOf("name" to json.string(name)).plus(description?.let { "description" to json.string(it) }.asList()))
}

private data class FieldsAndDefinitions<NODE>(val fields: List<Pair<String, NODE>> = emptyList(), val definitions: Set<Pair<String, NODE>> = emptySet()) {
    fun add(newField: Pair<String, NODE>, newDefinitions: Set<Pair<String, NODE>>) = FieldsAndDefinitions(fields.plus(newField), newDefinitions.plus(definitions))

    fun add(fieldAndDefinitions: FieldAndDefinitions<NODE>) = FieldsAndDefinitions(fields.plus(fieldAndDefinitions.field),
        fieldAndDefinitions.definitions.plus(definitions))
}

private data class FieldAndDefinitions<out NODE>(val field: Pair<String, NODE>, val definitions: Set<Pair<String, NODE>>)

private fun <T> T?.asList() = this?.let(::listOf) ?: listOf()
