package org.http4k.util

import org.http4k.format.JsonLibAutoMarshallingJson
import org.http4k.format.JsonType
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.ArrayParam
import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.IntegerParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.StringParam

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

    private fun Pair<String, NODE>.fieldsToSchema() =
        json.fields(second).map { field ->
            field.first to when (json.typeOf(field.second)) {
                JsonType.String -> field.toSchema(StringParam)
                JsonType.Integer -> field.toSchema(IntegerParam)
                JsonType.Number -> field.toSchema(NumberParam)
                JsonType.Boolean -> field.toSchema(BooleanParam)
                JsonType.Array -> toArraySchema()
                JsonType.Object -> toSchema(field.second, field.first)
                JsonType.Null -> throw IllegalSchemaException("Cannot use a null value in a schema!")
                else -> throw IllegalSchemaException("unknown type of ${field.first}")
            }
        }

    private fun Pair<String, NODE>.toSchema(paramMeta: ParamMeta) = SchemaNode.Primitive(paramMeta, second).toSchema()

    private fun Pair<String, NODE>.toArraySchema() = SchemaNode.Array(second).toSchema()

    private fun SchemaNode.toSchema() = JsonSchema(json.asJsonObject(this))
}

private sealed class SchemaNode(paramMeta: ParamMeta, val example: Any?) {
    val type = paramMeta.value

    class Primitive(paramMeta: ParamMeta, example: Any?) : SchemaNode(paramMeta, example)

    class Array(example: Any?) : SchemaNode(ArrayParam, example)
}
