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
    private val refPrefix: String = "components/schema"
) : JsonSchemaCreator<Any, NODE> {

    override fun toSchema(obj: Any, overrideDefinitionId: String?) = json {
        val schema = json.asJsonObject(obj).toObjectSchema(overrideDefinitionId ?: obj.javaClass.simpleName, obj, false)
        JsonSchema(
            obj(schema.name() to json.asJsonObject(schema)),
            schema.definitions().map { it.name() to json.asJsonObject(it) }.toSet())
    }

    private fun NODE.toSchema(name: String, paramMeta: ParamMeta, isNullable: Boolean) =
        SchemaNode.Primitive(name, paramMeta, isNullable, this)

    private fun NODE.toArraySchema(name: String, obj: Any, isNullable: Boolean): SchemaNode.Array {
        val items = when (obj) {
            is Array<*> -> obj.asList()
            is Iterable<*> -> obj.toList()
            else -> listOf(obj)
        }.filterNotNull()

        println(items)
        val schemas: List<Pair<JsonType, SchemaNode>> = json.elements(this)
            .mapIndexed { index, node ->
                json.typeOf(node) to node.toObjectSchema("", items[index], false)
            }

        return SchemaNode.Array(name, isNullable, Items(schemas), this)
    }

    private fun NODE.toObjectSchema(objName: String, obj: Any, isNullable: Boolean): SchemaNode {
        val fields = obj::class.memberProperties.toList()
        val properties = json.fields(this).mapIndexed { index, (fieldName, field) ->
            val kfield = fields[index]
            val fieldIsNullable = kfield.returnType.isMarkedNullable
            objName to when (json.typeOf(field)) {
                JsonType.String -> field.toSchema(fieldName, StringParam, fieldIsNullable)
                JsonType.Integer -> field.toSchema(fieldName, IntegerParam, fieldIsNullable)
                JsonType.Number -> field.toSchema(fieldName, NumberParam, fieldIsNullable)
                JsonType.Boolean -> field.toSchema(fieldName, BooleanParam, fieldIsNullable)
                JsonType.Array -> field.toArraySchema(fieldName, kfield.javaGetter!!.invoke(obj), fieldIsNullable)
                JsonType.Object -> field.toObjectSchema(fieldName, kfield.javaGetter!!.invoke(obj), fieldIsNullable)
                JsonType.Null -> throw IllegalSchemaException("Cannot use a null value in a schema!")
                else -> throw IllegalSchemaException("unknown type")
            }
        }.map { it.first to it.second }.toMap()

        return SchemaNode.Reference("#/$refPrefix/$objName", SchemaNode.Object(objName, isNullable, properties, this))
    }
}

sealed class ArrayItem {
    object Object : ArrayItem()
    data class NonObject(val type: String) : ArrayItem()
    data class Ref(val `$ref`: String) : ArrayItem()
}

private class Items(private val schemas: List<Pair<JsonType, SchemaNode>>) {
    val oneOf = schemas.map { it.second.arrayItem() }.also { println(it) }.toSet().sortedBy { it.toString() }
    fun definitions() = schemas.map { it.second }
}

private sealed class SchemaNode(
    private val name: String,
    private val paramMeta: ParamMeta,
    private val isNullable: Boolean, val example: Any?) {
    open fun definitions(): Iterable<SchemaNode> = emptyList()

    fun name() = name

    fun nonDisplayableType() = paramMeta.value
    abstract fun arrayItem(): ArrayItem

    class Primitive(name: String, paramMeta: ParamMeta, isNullable: Boolean, example: Any?) :
        SchemaNode(name, paramMeta, isNullable, example) {
        val type = nonDisplayableType()
        override fun arrayItem() = ArrayItem.NonObject(type)
    }

    class Array(name: String, isNullable: Boolean, val items: Items, example: Any?) :
        SchemaNode(name, ArrayParam, isNullable, example) {
        override fun arrayItem() = ArrayItem.NonObject(nonDisplayableType())
        override fun definitions() = items.definitions()
    }

    class Object(name: String, isNullable: Boolean, val properties: Map<String, SchemaNode>,
                 example: Any?) : SchemaNode(name, ObjectParam, isNullable, example) {
        val required = properties.filterNot { it.value.isNullable }.keys.sorted()
        override fun arrayItem() = ArrayItem.Object
    }

    class Reference(name: String, private val schemaNode: Object) :
        SchemaNode(name, ObjectParam, false, null) {
        override fun arrayItem() = ArrayItem.Ref(name())
        override fun definitions() = listOf(schemaNode)
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