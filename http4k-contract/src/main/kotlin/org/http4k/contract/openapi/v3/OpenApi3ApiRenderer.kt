package org.http4k.contract.openapi.v3

import org.http4k.contract.Tag
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.contract.openapi.v3.BodyContent.FormContent
import org.http4k.contract.openapi.v3.BodyContent.NoSchema
import org.http4k.contract.openapi.v3.BodyContent.SchemaContent
import org.http4k.contract.openapi.v3.RequestParameter.PrimitiveParameter
import org.http4k.contract.openapi.v3.RequestParameter.SchemaParameter
import org.http4k.format.Json
import org.http4k.util.JsonSchema
import org.http4k.util.JsonToJsonSchema

/**
 * Converts a API to OpenApi3 format JSON.
 */
class OpenApi3ApiRenderer<NODE>(private val json: Json<NODE>) : ApiRenderer<Api<NODE>, NODE> {
    private val jsonToJsonSchema = JsonToJsonSchema(json, "components/schemas")

    override fun api(api: Api<NODE>): NODE =
        with(api) {
            json {
                obj(
                    "openapi" to string(openapi),
                    "info" to info.asJson(),
                    "tags" to array(tags.map { it.asJson() }),
                    "paths" to paths.asJson(),
                    "components" to components.asJson()
                )
            }
        }

    private fun Tag.asJson(): NODE =
        json {
            obj(
                listOf(
                    "name" to string(name),
                    "description" to description.asJson()
                )
            )
        }

    private fun Components<NODE>.asJson() = json {
        obj(
            "schemas" to schemas,
            "securitySchemes" to securitySchemes
        )
    }

    private fun Map<String, Map<String, ApiPath<NODE>>>.asJson(): NODE =
        json {
            obj(
                map {
                    it.key to obj(
                        it.value
                            .map { it.key to it.value.toJson() }.sortedBy { it.first }
                    )
                }.sortedBy { it.first }
            )
        }

    private fun ApiPath<NODE>.toJson(): NODE =
        json {
            obj(
                "summary" to summary.asJson(),
                "description" to (description.asJson()),
                "tags" to (array(tags.map { string(it) })),
                "parameters" to parameters.asJson(),
                "requestBody" to (requestBody.asJson()),
                "responses" to responses.asJson(),
                "security" to (security),
                "operationId" to operationId.asJson()
            )
        }

    private fun RequestContents<NODE>.asJson(): NODE = json {
        obj(
            "content" to content.asJson(),
            "required" to boolean(content.isNotEmpty())
        )
    }

    @JvmName("contentAsJson")
    private fun Map<String, BodyContent>.asJson(): NODE = json {
        obj(
            map {
                it.key to (
                    listOf(it.value).filterIsInstance<NoSchema>().map { it.toJson() } +
                        listOf(it.value).filterIsInstance<SchemaContent<NODE>>().map { it.toJson() } +
                        listOf(it.value).filterIsInstance<FormContent>().map { it.toJson() }
                    ).firstOrNull().orNullNode()
            }
        )
    }

    private fun NoSchema.toJson(): NODE = json {
        obj("schema" to obj(schema.map { it.key to string(it.value) }))
    }

    private fun SchemaContent<NODE>.toJson(): NODE = json {
        obj(
            listOfNotNull(
                example?.let { "example" to it },
                schema?.let { "schema" to it }
            )
        )
    }

    private fun FormContent.toJson(): NODE = json {
        obj("schema" to
            obj(
                "type" to string("object"),
                "properties" to obj(
                    schema.properties.map {
                        it.key to obj(it.value.map { it.key to it.value.asJson() })
                    }
                ),
                "required" to array(schema.required.map { it.asJson() })
            )
        )
    }

    @JvmName("responseAsJson")
    private fun Map<String, ResponseContents<NODE>>.asJson(): NODE = json {
        obj(map {
            it.key to
                obj(
                    "description" to it.value.description.asJson(),
                    "content" to it.value.content.asJson())
        })
    }

    private fun List<RequestParameter<NODE>>.asJson(): NODE = json {
        array(
            filterIsInstance<SchemaParameter<NODE>>().map { it.asJson() }
                + filterIsInstance<PrimitiveParameter<NODE>>().map { it.asJson() }
        )
    }

    private fun SchemaParameter<NODE>.asJson(): NODE = json {
        obj(
            listOfNotNull(
                schema?.let { "schema" to it },
                "in" to string(`in`),
                "name" to string(name),
                "required" to boolean(required),
                "description" to description.asJson()
            )
        )
    }

    private fun PrimitiveParameter<NODE>.asJson(): NODE = json {
        obj(
            "schema" to schema,
            "in" to string(`in`),
            "name" to string(name),
            "required" to boolean(required),
            "description" to description.asJson()
        )
    }

    private fun ApiInfo.asJson() = json {
        obj("title" to string(title), "version" to string(version), "description" to string(description ?: ""))
    }

    private fun String?.asJson() = this?.let { json.string(it) } ?: json.nullNode()
    private fun NODE?.orNullNode() = this ?: json.nullNode()

    @Suppress("UNCHECKED_CAST")
    override fun toSchema(obj: Any, overrideDefinitionId: String?): JsonSchema<NODE> =
        try {
            jsonToJsonSchema.toSchema(obj as NODE, overrideDefinitionId)
        } catch (e: ClassCastException) {
            jsonToJsonSchema.toSchema(json.obj(), overrideDefinitionId)
        }

}
