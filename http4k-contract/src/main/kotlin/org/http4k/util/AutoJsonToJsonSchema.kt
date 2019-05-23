package org.http4k.util

import org.http4k.format.JsonLibAutoMarshallingJson
import org.http4k.format.JsonType
import org.http4k.lens.ParamMeta

class AutoJsonToJsonSchema<NODE : Any>(
    private val json: JsonLibAutoMarshallingJson<NODE>,
    private val refPrefix: String = "definitions"
) : JsonSchemaCreator<Any, NODE> {

    override fun toSchema(obj: Any, overrideDefinitionId: String?): JsonSchema<NODE> {
        println(json.asJsonObject(obj))
        val pair = (overrideDefinitionId ?: "foo") to json.asJsonObject(obj)
        return json {
            JsonSchema(
                obj(
                    (overrideDefinitionId ?: obj.javaClass.simpleName) to obj(
                        "properties" to obj(
                            pair.fieldsToSchema()
                                .also { println(it) }
                                .map { it.first to it.second.node }
                        )
                    )
                ), emptySet())
        }
    }

    private fun Pair<String, NODE>.fieldsToSchema() = json.fields(second).map { field ->
        println(field.toString() + " " + json.typeOf(second))
        field.first to when (json.typeOf(field.second)) {
            JsonType.String -> field.toStringSchema()
            JsonType.Integer -> field.toIntegerSchema()
            JsonType.Number -> field.toDecimalSchema()
            JsonType.Boolean -> field.toBooleanSchema()
            JsonType.Array -> field.toArraySchema()
            JsonType.Object -> toSchema(field.second, field.first)
            JsonType.Null -> throw IllegalSchemaException("Cannot use a null value in a schema!")
            else -> throw IllegalSchemaException("unknown type of ${field.first}")
        }
    }

    private fun Pair<String, NODE>.toStringSchema(): JsonSchema<NODE> = JsonSchema(
        json { obj("type" to string(ParamMeta.StringParam.value), "example" to second) },
        emptySet()
    )

    private fun Pair<String, NODE>.toIntegerSchema(): JsonSchema<NODE> = JsonSchema(
        json { obj("type" to string(ParamMeta.IntegerParam.value), "example" to second) },
        emptySet()
    )

    private fun Pair<String, NODE>.toDecimalSchema(): JsonSchema<NODE> = JsonSchema(
        json { obj("type" to string(ParamMeta.NumberParam.value), "example" to second) },
        emptySet()
    )

    private fun Pair<String, NODE>.toBooleanSchema(): JsonSchema<NODE> = JsonSchema(
        json { obj("type" to string(ParamMeta.BooleanParam.value), "example" to second) },
        emptySet()
    )

    private fun Pair<String, NODE>.toArraySchema(): JsonSchema<NODE> = JsonSchema(
        json { obj("type" to string(ParamMeta.ArrayParam.value), "example" to second) },
        emptySet()
    )
}