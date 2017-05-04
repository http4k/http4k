package org.reekwest.http.contract.module

import argo.jdom.JsonNode
import org.reekwest.http.contract.util.JsonToJsonSchema
import org.reekwest.http.core.Response
import org.reekwest.http.core.Status.Companion.OK
import org.reekwest.http.formats.Json
import org.reekwest.http.formats.JsonErrorResponseRenderer
import org.reekwest.http.lens.Failure

data class ApiInfo(val title: String, val version: String, val description: String? = null)

typealias Field = Pair<String, JsonNode>

class Swagger<ROOT : NODE, out NODE : Any>(private val apiInfo: ApiInfo, private val json: Json<ROOT, NODE>) : ModuleRenderer {

    private val schemaGenerator = JsonToJsonSchema(json)
    private val errors = JsonErrorResponseRenderer(json)

    override fun badRequest(failures: List<Failure>): Response = errors.badRequest(failures)

    override fun notFound(): Response = errors.notFound()

    private data class FieldAndDefinitions(val field: Field, val definitions: List<Field>)

    private data class FieldsAndDefinitions(val fields: List<Field> = emptyList(), val definitions: List<Field> = emptyList()) {
        fun add(newField: Field, newDefinitions: List<Field>) = FieldsAndDefinitions(fields.plus(newField), newDefinitions.plus(definitions))

        fun add(fieldAndDefinitions: FieldAndDefinitions) = FieldsAndDefinitions(fields.plus(fieldAndDefinitions.field),
            fieldAndDefinitions.definitions.plus(definitions))
    }

    //
//    private fun render(parameters: Any, schema: Schema?): Iterable<JsonNode> {
//        listOf()
//    }
//        parameters.map(
//            parameter ->
//    obj(
//    "in" -> string(parameter.where),
//    "name" -> string(parameter.name),
//    "description" -> Option(parameter.description).map(string).getOrElse(nullNode()),
//    "required" -> boolean(parameter.required),
//    schema.map("schema" -> _.node).getOrElse("type" -> string(parameter.paramType.name))
//    )
//    )
//
//    private fun render(basePath: Path, security: Security, route: ServerRoute<_, _>): FieldAndDefinitions =
//        {
//            val FieldsAndDefinitions(responses, responseDefinitions) = render(route.routeSpec.responses)
//
//            val bodyParameters = route.routeSpec.body.flatMap(p -> Option (p.toList)).getOrElse(Nil)
//
//            val bodyAndSchemaAndRendered = bodyParameters.map(p -> {
//                val exampleOption = p.example.flatMap(s -> Try (parse(s)).toOption).map(schemaGenerator.toSchema)
//                (p, exampleOption, render(p, exampleOption))
//            })
//
//            val allParams = route.pathParams.flatten++ route . routeSpec . requestParams
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
//        }
//
//    private fun render(responses: List<ResponseSpec>): FieldsAndDefinitions =
//        responses.foldLeft(FieldsAndDefinitions())
//        {
//            data(memo, nextResp) ->
//            val newSchema = Try(parse(nextResp.example.get)).toOption.map(schemaGenerator.toSchema).getOrElse(Schema(nullNode(), Nil))
//            val newField = nextResp.status.code.toString -> obj("description" -> string(nextResp.description), "schema" -> newSchema.node)
//            memo.add(newField, newSchema.definitions)
//        }
//
//    private fun render(security: Security) = security match
//        {
//            data NoSecurity -> obj ()
//            data ApiKey (param, _) -> obj(List(
//            "api_key" -> obj(
//            "type" -> string("apiKey"),
//            "in" -> string(param.where),
//            "name" -> string(param.name)
//            )
//            ))
//        }
//
    private fun render(apiInfo: ApiInfo) =
        json.obj("title" to json.string(apiInfo.title),
            "version" to json.string(apiInfo.version),
            "description" to json.string(apiInfo.description ?: ""))

    //
    override fun description(moduleRoot: BasePath, security: Security, routes: List<ServerRoute>): Response {
//            val pathsAndDefinitions = routes
//                .groupBy{ it.describeFor(basePath) }
//                .fold(FieldsAndDefinitions()) {
//                    data(memo, (path, routesForThisPath)) ->
//                    val routeFieldsAndDefinitions = routesForThisPath.foldLeft(FieldsAndDefinitions()) {
//                        data(memoFields, route) -> memoFields.add(render(basePath, security, route))
//                    }
//                    memo.add(path -> obj(routeFieldsAndDefinitions.fields), routeFieldsAndDefinitions.definitions)
//                }

        return Response(OK).body(json.pretty(json.obj(
            "swagger" to json.string("2.0"),
            "info" to render(apiInfo),
            "basePath" to json.string("/")
//            "paths" to obj(pathsAndDefinitions.fields),
//            "securityDefinitions" to render(security),
//            "definitions" to obj(pathsAndDefinitions.definitions)
        )))
    }
}