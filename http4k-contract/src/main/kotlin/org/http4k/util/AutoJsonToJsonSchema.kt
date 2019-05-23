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
            val properties = pair.fieldsToSchema().map { it.first to it.second }.toMap()
            val s = SchemaNode.Object(properties, properties.keys, obj)
            JsonSchema(
                obj(
                    (overrideDefinitionId ?: obj.javaClass.simpleName) to json.asJsonObject(s)
                ), emptySet())
        }
    }

    private fun Pair<String, NODE>.fieldsToSchema(): List<Pair<String, SchemaNode>> =
        json.fields(second).map { field ->
            field.first to when (json.typeOf(field.second)) {
                JsonType.String -> field.toSchema(StringParam)
                JsonType.Integer -> field.toSchema(IntegerParam)
                JsonType.Number -> field.toSchema(NumberParam)
                JsonType.Boolean -> field.toSchema(BooleanParam)
                JsonType.Array -> field.toArraySchema()
                JsonType.Object -> field.toObjectSchema()
                JsonType.Null -> throw IllegalSchemaException("Cannot use a null value in a schema!")
                else -> throw IllegalSchemaException("unknown type of ${field.first}")
            }
        }

    private fun Pair<String, NODE>.toSchema(paramMeta: ParamMeta) = SchemaNode.Primitive(paramMeta, second)

    private fun Pair<String, NODE>.toArraySchema() = SchemaNode.Array(second)
    private fun Pair<String, NODE>.toObjectSchema() = SchemaNode.Object(emptyMap(), emptySet(), second)
}

private sealed class SchemaNode(val example: Any?) {
    class Primitive(paramMeta: ParamMeta, example: Any?) : SchemaNode(example) {
        val type = paramMeta.value
    }

    class Array(example: Any?) : SchemaNode(example) {
        val type = ArrayParam.value
    }

    class Object(val properties: Map<String, SchemaNode>,
                 required: Set<String>,
                 example: Any?) : SchemaNode(example) {
        val required = required.sorted()
    }
}
