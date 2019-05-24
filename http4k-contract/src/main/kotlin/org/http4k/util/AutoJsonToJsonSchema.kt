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
        val schema = json.asJsonObject(obj).toObjectSchema(overrideDefinitionId, obj, false)
        JsonSchema(
            json.asJsonObject(schema),
            schema.definitions().map { it.name() to json.asJsonObject(it) }.toSet())
    }

    private fun NODE.toSchema(name: String, paramMeta: ParamMeta, isNullable: Boolean) =
        SchemaNode.Primitive(name, paramMeta, isNullable, this)

    private fun NODE.toArraySchema(name: String, obj: Any, isNullable: Boolean): SchemaNode.Array {
        val items = items(obj)
        val schemas = json.elements(this).mapIndexed { index, node ->
            //            println("" + node + " -> " + items[index])
            when (val param = json.typeOf(node).toParam()) {
                ArrayParam -> node.toArraySchema("", items[index], false)
                ObjectParam -> node.toObjectSchema(null, items[index], false)
                else -> node.toSchema("", param, false)
            }
        }

        return SchemaNode.Array(name, isNullable, Items.Inner(schemas), this)
    }

    private fun items(obj: Any) = when (obj) {
        is Array<*> -> obj.asList()
        is Iterable<*> -> obj.toList()
        else -> listOf(obj)
    }.filterNotNull()

    private fun NODE.toObjectSchema(objName: String?, obj: Any, isNullable: Boolean): SchemaNode {
        val fields = obj::class.memberProperties.map { it.name to it }.toMap()
        val name = objName ?: obj.javaClass.simpleName
        val properties = json.fields(this).map { (fieldName, field) ->
            val kField = fields.getValue(fieldName)
            println(fieldName + " -> " + kField.javaGetter!!(obj))
            val fieldIsNullable = kField.returnType.isMarkedNullable
            when (val param = json.typeOf(field).toParam()) {
                ArrayParam -> field.toArraySchema(fieldName, kField.javaGetter!!(obj), fieldIsNullable)
                ObjectParam -> field.toObjectSchema(fieldName, kField.javaGetter!!(obj), fieldIsNullable)
                else -> field.toSchema(fieldName, param, fieldIsNullable)
            }
        }.map { it.name() to it }.toMap()

        return SchemaNode.Reference(name, "#/$refPrefix/${obj.javaClass.simpleName}",
            SchemaNode.Object(obj.javaClass.simpleName, isNullable, properties, this))
    }
}

private sealed class ArrayItem {
    object Array : ArrayItem() {
        val type = ArrayParam.value
    }

    class NonObject(paramMeta: ParamMeta) : ArrayItem() {
        val type = paramMeta.value
    }

    data class Ref(val `$ref`: String) : ArrayItem()
}

private sealed class Items {
    abstract fun definitions(): List<SchemaNode>

    class Nested(val items: Items) : Items() {
        override fun definitions() = items.definitions()
    }

    class Inner(private val schemas: List<SchemaNode>) : Items() {
        val oneOf = schemas.map { it.arrayItem() }.toSet().sortedBy { it.toString() }
        override fun definitions() = schemas.flatMap { it.definitions() }
    }
}

private sealed class SchemaNode(
    private val name: String,
    private val paramMeta: ParamMeta,
    private val isNullable: Boolean, val example: Any?) {
    open fun definitions(): Iterable<SchemaNode> = emptyList()

    fun name() = name

    fun paramMeta() = paramMeta
    abstract fun arrayItem(): ArrayItem

    class Primitive(name: String, paramMeta: ParamMeta, isNullable: Boolean, example: Any?) :
        SchemaNode(name, paramMeta, isNullable, example) {
        val type = paramMeta().value
        override fun arrayItem() = ArrayItem.NonObject(paramMeta())
    }

    class Array(name: String, isNullable: Boolean, val items: Items, example: Any?) :
        SchemaNode(name, ArrayParam, isNullable, example) {
        val type = paramMeta().value
        override fun arrayItem() = when (paramMeta()) {
            ArrayParam -> ArrayItem.Array
            ObjectParam -> ArrayItem.Ref(name())
            else -> ArrayItem.NonObject(paramMeta())
        }

        override fun definitions() = items.definitions()
    }

    class Object(name: String, isNullable: Boolean, val properties: Map<String, SchemaNode>,
                 example: Any?) : SchemaNode(name, ObjectParam, isNullable, example) {
        val type = paramMeta().value
        val required = properties.filterNot { it.value.isNullable }.keys.sorted()
        override fun arrayItem() = ArrayItem.Ref(name())
        override fun definitions() = properties.values.flatMap { it.definitions() }
    }

    class Reference(name: String,
                    val `$ref`: String,
                    private val schemaNode: Object) : SchemaNode(name, ObjectParam, false, null) {
        override fun arrayItem() = ArrayItem.Ref(`$ref`)
        override fun definitions() = listOf(schemaNode) + schemaNode.definitions()
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