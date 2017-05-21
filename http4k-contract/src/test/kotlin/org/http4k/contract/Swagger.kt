package org.http4k.contract

import org.http4k.core.HttpMessage
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.format.Json
import org.http4k.format.JsonErrorResponseRenderer
import org.http4k.lens.Failure
import org.http4k.lens.Meta
import util.JsonSchema
import util.JsonToJsonSchema

data class ApiInfo(val title: String, val version: String, val description: String? = null)

class Swagger<ROOT : NODE, out NODE : Any>(private val apiInfo: ApiInfo, private val json: Json<ROOT, NODE>) : ModuleRenderer {

    private val schemaGenerator = JsonToJsonSchema(json)
    private val errors = JsonErrorResponseRenderer(json)

    override fun badRequest(failures: List<Failure>): Response = errors.badRequest(failures)

    override fun notFound(): Response = errors.notFound()

    private data class FieldAndDefinitions<out NODE : Any>(val field: Pair<String, NODE>, val definitions: List<Pair<String, NODE>>)

    private data class FieldsAndDefinitions<NODE : Any>(val fields: List<Pair<String, NODE>> = emptyList(), val definitions: List<Pair<String, NODE>> = emptyList()) {
        fun add(newField: Pair<String, NODE>, newDefinitions: List<Pair<String, NODE>>) = FieldsAndDefinitions(fields.plus(newField), newDefinitions.plus(definitions))

        fun add(fieldAndDefinitions: FieldAndDefinitions<NODE>) = FieldsAndDefinitions(fields.plus(fieldAndDefinitions.field),
            fieldAndDefinitions.definitions.plus(definitions))
    }


    private fun render(parameters: Iterable<Meta>, schema: JsonSchema<NODE>?) =
        parameters.map {
            json.obj(
                "in" to json.string(it.location),
                "name" to json.string(it.name),
                "description" to (it.description?.let(json::string) ?: json.nullNode()),
                "required" to json.boolean(it.required),
                schema?.let { "schema" to it.node } ?: "type" to json.string(it.paramMeta.value)
            )
        }

    private fun <T> T?.asList() = this?.let { listOf(it) } ?: listOf()

    private fun fetchTags(routes: List<ServerRoute>) = routes
        .flatMap { it.core.tags }
        .toSet()
        .sortedBy { it.name }

    private fun renderTags(tags: List<Tag>) = json.array(tags.map {
        json.obj(listOf("name" to json.string(it.name)).plus(it.description?.let { "description" to json.string(it) }.asList()))
    })

    private fun render(basePath: BasePath, security: Security, route: ServerRoute): FieldAndDefinitions<NODE> {
        val (responses, responseDefinitions) = render(route.core.responses.values.toList())

        val nonBodyParams = route.allParams.flatMap { render(listOf(it), null) }

        val tags = if (route.core.tags.isEmpty()) listOf(Tag(basePath.toString())) else route.core.tags.toList().sortedBy { it.name }

        val jsonRoute = json.obj(
            "tags" to json.array(tags.map { json.string(it.name) }),
            "summary" to json.string(route.core.summary),
            "description" to (route.core.description?.let(json::string) ?: json.nullNode()),
            "produces" to json.array(route.core.produces.map { json.string(it.value) }),
            "consumes" to json.array(route.core.consumes.map { json.string(it.value) }),
            "parameters" to json.array(nonBodyParams),
            "responses" to json.obj(responses),
            "supportedContentTypes" to json.array(route.core.produces.map { json.string(it.value) }),
            "security" to json.array(when (security) {
                is ApiKey<*> -> listOf(json.obj("api_key" to json.array(emptyList())))
                else -> emptyList<NODE>()
            })
        )

        val definitions = route.core.request.asList().flatMap { schemaFor(it).definitions }.plus(responseDefinitions)

        return FieldAndDefinitions(route.method.toString().toLowerCase() to jsonRoute, definitions.distinct())
    }

    private fun render(responses: List<Pair<String, Response>>) =
        responses.fold(FieldsAndDefinitions<NODE>(),
            {
                memo, (reason, response) ->
                val newSchema = schemaFor(response)
                val newField = response.status.code.toString() to json.obj(
                    "description" to json.string(reason),
                    "schema" to newSchema.node)
                memo.add(newField, newSchema.definitions)
            })

    private fun schemaFor(message: HttpMessage): JsonSchema<NODE> = try {
        schemaGenerator.toSchema(json.parse(message.bodyString()))
    } catch (e: Exception) {
        JsonSchema(json.nullNode(), emptyList())
    }

    private fun render(security: Security) = when (security) {
        is ApiKey<*> -> json.obj(
            "api_key" to json.obj(
                "type" to json.string("apiKey"),
                "in" to json.string(security.param.meta.location),
                "name" to json.string(security.param.meta.name)
            ))
        else -> json.obj(listOf())
    }

    private fun render(apiInfo: ApiInfo) =
        json.obj("title" to json.string(apiInfo.title),
            "version" to json.string(apiInfo.version),
            "description" to json.string(apiInfo.description ?: ""))

    override fun description(moduleRoot: BasePath, security: Security, routes: List<ServerRoute>): Response {
        val pathsAndDefinitions = routes
            .groupBy { it.describeFor(moduleRoot) }.entries
            .fold(FieldsAndDefinitions<NODE>(), {
                memo, (path, routes) ->
                val routeFieldsAndDefinitions = routes.fold(FieldsAndDefinitions<NODE>(), {
                    memoFields, route ->
                    memoFields.add(render(moduleRoot, security, route))
                })
                memo.add(path to json.obj(routeFieldsAndDefinitions.fields), routeFieldsAndDefinitions.definitions)
            })

        return Response(OK).body(json.pretty(json.obj(
            "swagger" to json.string("2.0"),
            "info" to render(apiInfo),
            "basePath" to json.string("/"),
            "tags" to renderTags(fetchTags(routes)),
            "paths" to json.obj(pathsAndDefinitions.fields),
            "securityDefinitions" to render(security),
            "definitions" to json.obj(pathsAndDefinitions.definitions)
        )))
    }
}