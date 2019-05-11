package org.http4k.contract.openapi.v3

import org.http4k.contract.Tag
import org.http4k.contract.openapi.ApiInfo
import org.http4k.contract.openapi.ApiRenderer
import org.http4k.contract.openapi.v3.RequestParameter.PrimitiveParameter
import org.http4k.contract.openapi.v3.RequestParameter.SchemaParameter
import org.http4k.format.Json
import org.http4k.util.JsonSchema
import org.http4k.util.JsonToJsonSchema

class StandardApiRenderer<NODE>(private val json: Json<NODE>) : ApiRenderer<Api<NODE>, NODE> {
    private val jsonToJsonSchema = JsonToJsonSchema(json)

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
                "tags" to (tags?.map { string(it) }?.let { array(it) }.orNullNode()),
                "parameters" to (parameters?.asJson().orNullNode()),
                "requestBody" to (requestBody?.asJson().orNullNode()),
                "responses" to responses.asJson(),
                "security" to (security.orNullNode()),
                "operationId" to operationId.asJson()
            )
        }


    private fun RequestContents<NODE>.asJson(): NODE = json {
        obj("contents" to (content
            ?.map { it.key to it.value.asJson() }
            ?.let { obj(it) }.orNullNode()))
    }

    private fun ResponseContents<NODE>.asJson(): NODE = json {
        obj(
            "description" to description.asJson(),
            "contents" to (obj(content.map { it.key to it.value.asJson() }).orNullNode()))
    }

    private fun BodyContent.asJson(): NODE = json {
        obj()
    }

    @JvmName("responseAsJson")
    private fun Map<String, ResponseContents<NODE>>.asJson(): NODE = json {
        obj(map { it.key to it.value.asJson() })
    }

    private fun List<RequestParameter<NODE>>.asJson(): NODE = json {
        array(
            filterIsInstance<SchemaParameter<NODE>>().map { it.asJson() }
                + filterIsInstance<PrimitiveParameter<NODE>>().map { it.asJson() }
        )
    }

    private fun SchemaParameter<NODE>.asJson(): NODE = json {
        obj(
            "in" to string(`in`),
            "name" to string(name),
            "required" to boolean(required),
            "description" to (description.asJson()),
            "schema" to (schema.orNullNode())
        )
    }

    private fun PrimitiveParameter<NODE>.asJson(): NODE = json {
        obj(
            "in" to string(`in`),
            "name" to string(name),
            "required" to boolean(required),
            "description" to (description?.let { string(it) }.orNullNode()),
            "schema" to schema
        )
    }

    private fun Tag.asJson(): NODE =
        json {
            obj(
                listOf(
                    "name" to string(name),
                    "description" to (description?.let { json.string(it) }.orNullNode())
                )
            )
        }

    private fun ApiInfo.asJson() = json {
        obj("title" to string(title), "version" to string(version), "description" to string(description ?: ""))
    }

    private fun String?.asJson() = this?.let { json.string(it) } ?: json.nullNode()
    private fun NODE?.orNullNode() = this ?: json.nullNode()

    @Suppress("UNCHECKED_CAST")
    override fun toSchema(obj: Any, overrideDefinitionId: String?): JsonSchema<NODE> =
        jsonToJsonSchema.toSchema(obj as NODE, overrideDefinitionId)
}
