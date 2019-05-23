package org.http4k.util

import org.http4k.format.JsonLibAutoMarshallingJson
import org.http4k.format.JsonType
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.ArrayParam
import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.IntegerParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.lens.ParamMeta.StringParam
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter

class AutoJsonToJsonSchema<NODE : Any>(
    private val json: JsonLibAutoMarshallingJson<NODE>,
    private val refPrefix: String = "definitions"
) : JsonSchemaCreator<Any, NODE> {

    override fun toSchema(obj: Any, overrideDefinitionId: String?) = json {
        JsonSchema(
            obj(
                (overrideDefinitionId ?: obj.javaClass.simpleName) to
                    json.asJsonObject(json.asJsonObject(obj).toObjectSchema(obj, false))
            ), emptySet())
    }

    private fun NODE.toSchema(paramMeta: ParamMeta, isNullable: Boolean) = SchemaNode.Primitive(paramMeta, isNullable, this)

    private fun NODE.toArraySchema(obj: Any, isNullable: Boolean): SchemaNode.Array {
        val items = when (obj) {
            is Array<*> -> obj.asList()
            is Iterable<*> -> obj.toList()
            else -> listOf(obj)
        }.filterNotNull()

        println(items)
        val schemas: List<Pair<JsonType, SchemaNode>> = json.elements(this)
            .mapIndexed { index, node ->
                json.typeOf(node) to node.toObjectSchema(items[index], false)
            }

        return SchemaNode.Array(isNullable, Items(schemas), this)
    }

    private fun NODE.toObjectSchema(obj: Any, isNullable: Boolean): SchemaNode {
        val fields = obj::class.memberProperties.toList()
        val properties = json.fields(this).mapIndexed { index, (name, field) ->
            val kfield = fields[index]
            val fieldIsNullable = kfield.returnType.isMarkedNullable
            name to when (json.typeOf(field)) {
                JsonType.String -> field.toSchema(StringParam, fieldIsNullable)
                JsonType.Integer -> field.toSchema(IntegerParam, fieldIsNullable)
                JsonType.Number -> field.toSchema(NumberParam, fieldIsNullable)
                JsonType.Boolean -> field.toSchema(BooleanParam, fieldIsNullable)
                JsonType.Array -> field.toArraySchema(kfield.javaGetter!!.invoke(obj), fieldIsNullable)
                JsonType.Object -> field.toObjectSchema(kfield.javaGetter!!.invoke(obj), fieldIsNullable)
                JsonType.Null -> throw IllegalSchemaException("Cannot use a null value in a schema!")
                else -> throw IllegalSchemaException("unknown type")
            }
        }.map { it.first to it.second }.toMap()

        return SchemaNode.Object(isNullable, properties, this)
    }
}

sealed class ArrayItem {
    object Object : ArrayItem()
    data class NonObject(val type: String) : ArrayItem()
}

private class Items(schemas: List<Pair<JsonType, SchemaNode>>) {
    init {
        println(schemas)
    }

    val oneOf = schemas.map { it.second }.map { it.arrayItem() }.also { println(it) }.toSet().sortedBy { it.toString() }
}

private sealed class SchemaNode(
    private val paramMeta: ParamMeta,
    private val isNullable: Boolean, val example: Any?) {

    fun nonDisplayableType() = paramMeta.value
    abstract fun arrayItem(): ArrayItem

    class Primitive(paramMeta: ParamMeta, isNullable: Boolean, example: Any?) : SchemaNode(paramMeta, isNullable, example) {
        val type = nonDisplayableType()
        override fun arrayItem() = ArrayItem.NonObject(type)
    }

    class Array(isNullable: Boolean, val items: Items, example: Any?) : SchemaNode(ArrayParam, isNullable, example) {
        override fun arrayItem() = ArrayItem.NonObject(nonDisplayableType())
    }

    class Object(isNullable: Boolean, val properties: Map<String, SchemaNode>,
                 example: Any?) : SchemaNode(ObjectParam, isNullable, example) {
        val required = properties.filterNot { it.value.isNullable }.keys.sorted()
        override fun arrayItem() = ArrayItem.Object
    }
}

private fun JsonType.toParam() = when (this) {
    JsonType.String -> StringParam
    JsonType.Integer -> IntegerParam
    JsonType.Number -> NumberParam
    JsonType.Boolean -> BooleanParam
    JsonType.Array -> ArrayParam
    JsonType.Object -> ObjectParam
    JsonType.Null -> throw IllegalSchemaException("Cannot use a null value in a schema!")
    else -> throw IllegalSchemaException("unknown type")
}