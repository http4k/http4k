package org.http4k.contract

import org.http4k.http.core.Response
import org.http4k.http.core.Status.Companion.OK
import org.http4k.http.formats.Json
import org.http4k.http.formats.JsonErrorResponseRenderer
import org.http4k.http.lens.Failure
import org.http4k.http.lens.Meta
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
                schema?.let { "schema" to it.node } ?: "type" to json.string(it.name)
            )
        }

    private fun render(moduleRoot: BasePath, security: Security, route: ServerRoute): FieldAndDefinitions<NODE> {
        val (responses, responseDefinitions) = render(route.core.responses)
//
//        val bodyParameters = route.body.flatMap(p -> Option (p.toList)).getOrElse(Nil)
//
//            val bodyAndSchemaAndRendered = bodyParams.map {p ->
//                val exampleOption = p.example.flatMap(s -> Try (parse(s)).toOption).map(schemaGenerator.toSchema)
//                (p, exampleOption, render(p, exampleOption))
//            })
//
        val nonBodyParams = route.allParams.map { render(listOf(it), null) }

//            val nonBodyParams = allParams.flatMap(render(_, Option.empty))
//
//            val jsonRoute = route.method.toString().toLowerCase -> obj(
//            "tags" -> array(string(basePath.toString)),
//            "summary" -> string(route.routeSpec.summary),
//            "description" -> route.routeSpec.description.map(string).getOrElse(nullNode()),
//            "produces" -> array(route.routeSpec.produces.map(m -> string(m.value))),
//            "consumes" -> array(route.routeSpec.consumes.map(m -> string(m.value))),
//            "parameters" -> array(nonBodyParams++bodyAndSchemaAndRendered.flatMap(_._3)),
//            "responses" -> obj(responses),
//            "supportedContentTypes" -> array(route.routeSpec.produces.map(m -> string(m.value))),
//            "security" -> array(security match {
//            data NoSecurity -> Nil
//                data ApiKey (_, _) -> List(obj("api_key" -> array()))
//        })
//            )
//            FieldAndDefinitions(jsonRoute, responseDefinitions++ bodyAndSchemaAndRendered . flatMap (_._2).flatMap(_.definitions))
        return FieldAndDefinitions<NODE>("" to json.obj(listOf()), emptyList())
    }

    private fun render(responses: List<Pair<String, Response>>) =
        responses.fold(FieldsAndDefinitions<NODE>(),
            {
                memo, (reason, response) ->
                val newSchema = try {
                    schemaGenerator.toSchema(json.parse(response.bodyString()))
                } catch (e: Exception) {
                    JsonSchema(json.nullNode(), emptyList())
                }
                val newField = response.status.code.toString() to json.obj(
                    "description" to json.string(reason),
                    "schema" to newSchema.node)
                memo.add(newField, newSchema.definitions)
            })

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
            "paths" to json.obj(pathsAndDefinitions.fields),
            "securityDefinitions" to render(security),
            "definitions" to json.obj(pathsAndDefinitions.definitions)
        )))
    }
}