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

    override fun toSchema(obj: Any, overrideDefinitionId: String?) = json {
        JsonSchema(
            obj(
                (overrideDefinitionId ?: obj.javaClass.simpleName) to
                    json.asJsonObject(json.asJsonObject(obj).toObjectSchema())
            ), emptySet())
    }

    private fun NODE.fieldsToSchema(): List<Pair<String, SchemaNode>> =
        json.fields(this).map { (name, field) ->
            name to when (json.typeOf(field)) {
                JsonType.String -> field.toSchema(StringParam)
                JsonType.Integer -> field.toSchema(IntegerParam)
                JsonType.Number -> field.toSchema(NumberParam)
                JsonType.Boolean -> field.toSchema(BooleanParam)
                JsonType.Array -> field.toArraySchema()
                JsonType.Object -> field.toObjectSchema()
                JsonType.Null -> throw IllegalSchemaException("Cannot use a null value in a schema!")
                else -> throw IllegalSchemaException("unknown type")
            }
        }

    private fun NODE.toSchema(paramMeta: ParamMeta) = SchemaNode.Primitive(paramMeta, this)

    private fun NODE.toArraySchema() = SchemaNode.Array(this)

    private fun NODE.toObjectSchema(): SchemaNode.Object {
        val properties = fieldsToSchema().map { it.first to it.second }.toMap()
        return SchemaNode.Object(properties, properties.keys, this)
    }
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
