package org.http4k.contract.openapi.v3

import org.http4k.format.JsonLibAutoMarshallingJson
import org.http4k.format.JsonType
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.ArrayParam
import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.IntegerParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.util.IllegalSchemaException
import org.http4k.util.JsonSchema
import org.http4k.util.JsonSchemaCreator

class AutoJsonToJsonSchema<NODE : Any>(
    private val json: JsonLibAutoMarshallingJson<NODE>,
    private val fieldRetrieval: FieldRetrieval = FieldRetrieval.compose(SimpleLookup()),
    private val modelNamer: SchemaModelNamer = SchemaModelNamer.Simple,
    private val refPrefix: String = "components/schemas"
) : JsonSchemaCreator<Any, NODE> {

    override fun toSchema(obj: Any, overrideDefinitionId: String?): JsonSchema<NODE> {
        val schema = json.asJsonObject(obj).toSchema(obj, overrideDefinitionId, true)
        return JsonSchema(
            json.asJsonObject(schema),
            schema.definitions().map { it.name() to json.asJsonObject(it) }.distinctBy { it.first }.toSet())
    }

    private fun NODE.toSchema(value: Any, objName: String?, topLevel: Boolean) =
        when (val param = json.typeOf(this).toParam()) {
            ArrayParam -> toArraySchema("", value, false)
            ObjectParam -> toObjectOrMapSchema(objName, value, false, topLevel)
            else -> toSchema("", param, false)
        }

    private fun NODE.toSchema(name: String, paramMeta: ParamMeta, isNullable: Boolean) =
        SchemaNode.Primitive(name, paramMeta, isNullable, this)

    private fun NODE.toArraySchema(name: String, obj: Any, isNullable: Boolean): SchemaNode.Array {
        val items = Items(
            json.elements(this)
                .zip(items(obj)) { node: NODE, value: Any ->
                    value.javaClass.enumConstants?.let {
                        node.toEnumSchema("", it[0], json.typeOf(node).toParam(), it, false)
                    } ?: node.toSchema(value, null, false)
                }
        )

        return SchemaNode.Array(name, isNullable, items, this)
    }

    private fun NODE.toEnumSchema(fieldName: String, obj: Any, param: ParamMeta,
                                  enumConstants: Array<Any>, isNullable: Boolean): SchemaNode =
        SchemaNode.Reference(fieldName, "#/$refPrefix/${modelNamer(obj)}",
            SchemaNode.Enum(modelNamer(obj), param, isNullable, this, enumConstants.map { it.toString() }))


    private fun NODE.toObjectOrMapSchema(objName: String?, obj: Any, isNullable: Boolean, topLevel: Boolean) =
        if (obj is Map<*, *>) toMapSchema(objName, obj, isNullable, topLevel) else toObjectSchema(objName, obj, isNullable, topLevel)

    private fun NODE.toObjectSchema(objName: String?, obj: Any, isNullable: Boolean, topLevel: Boolean): SchemaNode.Reference {
        val properties = json.fields(this)
            .map { Triple(it.first, it.second, fieldRetrieval(obj, it.first)) }
            .map { (fieldName, field, kField) ->
                when (val param = json.typeOf(field).toParam()) {
                    ArrayParam -> field.toArraySchema(fieldName, kField.value, kField.isNullable)
                    ObjectParam -> field.toObjectOrMapSchema(fieldName, kField.value, kField.isNullable, false)
                    else -> with(field) {
                        kField.value.javaClass.enumConstants
                            ?.let { toEnumSchema(fieldName, kField.value, param, it, kField.isNullable) }
                            ?: toSchema(fieldName, param, kField.isNullable)
                    }
                }
            }
            .map { it.name() to it }.toMap()

        val nameToUseForRef = if (topLevel) objName ?: modelNamer(obj) else modelNamer(obj)

        return SchemaNode.Reference(objName
            ?: modelNamer(obj), "#/$refPrefix/$nameToUseForRef",
            SchemaNode.Object(nameToUseForRef, isNullable, properties, this))
    }

    private fun NODE.toMapSchema(objName: String?, obj: Map<*, *>, isNullable: Boolean, topLevel: Boolean): SchemaNode {
        val objWithStringKeys = obj.mapKeys { it.key?.let(::toJsonKey) }
        val properties = json.fields(this)
            .map { Triple(it.first, it.second, objWithStringKeys[it.first]!!) }
            .map { (fieldName, field, value) ->
                when (val param = json.typeOf(field).toParam()) {
                    ArrayParam -> field.toArraySchema(fieldName, value, false)
                    ObjectParam -> field.toObjectOrMapSchema(fieldName, value, false, false)
                    else -> with(field) {
                        value.javaClass.enumConstants?.let {
                            toEnumSchema(fieldName, value, param, it, false)
                        } ?: toSchema(fieldName, param, false)
                    }
                }
            }
            .map { it.name() to it }.toMap()

        return if (topLevel && objName != null) {
            SchemaNode.Reference(objName, "#/$refPrefix/$objName",
                SchemaNode.Object(objName, isNullable, properties, this)
            )
        } else
            SchemaNode.MapType(objName ?: modelNamer(obj), isNullable,
                SchemaNode.Object(modelNamer(obj), isNullable, properties, this))
    }

    private fun toJsonKey(it: Any): String {
        data class MapKey(val keyAsString: Any)
        return json.textValueOf(json.asJsonObject(MapKey(it)), "keyAsString")!!
    }
}

interface SchemaModelNamer : (Any) -> String {
    companion object {
        val Simple: SchemaModelNamer = object : SchemaModelNamer {
            override fun invoke(p1: Any) = p1.javaClass.simpleName
        }
        val Full: SchemaModelNamer = object : SchemaModelNamer {
            override fun invoke(p1: Any) = p1.javaClass.name
        }
    }
}

private sealed class ArrayItem {
    data class Array(val items: Items) : ArrayItem() {
        val type = ArrayParam.value
    }

    class NonObject(paramMeta: ParamMeta) : ArrayItem() {
        val type = paramMeta.value
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as NonObject

            if (type != other.type) return false

            return true
        }

        override fun hashCode(): Int = type.hashCode()
    }

    data class Ref(val `$ref`: String) : ArrayItem()
}

private class Items(private val schemas: List<SchemaNode>) {
    val oneOf = schemas.map { it.arrayItem() }.toSet().sortedBy { it.javaClass.simpleName }

    fun definitions() = schemas.flatMap { it.definitions() }
}

private sealed class SchemaNode(
    private val _name: String,
    private val _paramMeta: ParamMeta,
    private val isNullable: Boolean,
    val example: Any?) {
    abstract fun definitions(): Iterable<SchemaNode>

    fun name() = _name

    fun paramMeta() = _paramMeta
    abstract fun arrayItem(): ArrayItem

    class Primitive(name: String, paramMeta: ParamMeta, isNullable: Boolean, example: Any?) :
        SchemaNode(name, paramMeta, isNullable, example) {
        val type = paramMeta().value
        override fun arrayItem() = ArrayItem.NonObject(paramMeta())
        override fun definitions() = emptyList<SchemaNode>()
    }

    class Enum(name: String, paramMeta: ParamMeta, isNullable: Boolean, example: Any?, val enum: List<String>) :
        SchemaNode(name, paramMeta, isNullable, example) {
        val type = paramMeta().value
        override fun arrayItem() = ArrayItem.Ref(name())
        override fun definitions() = emptyList<SchemaNode>()
    }

    class Array(name: String, isNullable: Boolean, val items: Items, example: Any?) :
        SchemaNode(name, ArrayParam, isNullable, example) {
        val type = paramMeta().value

        override fun arrayItem() = when (paramMeta()) {
            ArrayParam -> ArrayItem.Array(items)
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
                    private val schemaNode: SchemaNode) : SchemaNode(name, ObjectParam, schemaNode.isNullable, null) {
        override fun arrayItem() = ArrayItem.Ref(`$ref`)
        override fun definitions() = listOf(schemaNode) + schemaNode.definitions()
    }

    class MapType(name: String, isNullable: Boolean, val additionalProperties: SchemaNode) : SchemaNode(name, ObjectParam, isNullable, null) {
        val type = paramMeta().value
        override fun arrayItem() = ArrayItem.Ref(name())
        override fun definitions() = additionalProperties.definitions()
    }
}

private fun items(obj: Any) = when (obj) {
    is Array<*> -> obj.asList()
    is Iterable<*> -> obj.toList()
    else -> listOf(obj)
}.filterNotNull()

private fun JsonType.toParam() = when (this) {
    JsonType.String -> StringParam
    JsonType.Integer -> IntegerParam
    JsonType.Number -> NumberParam
    JsonType.Boolean -> BooleanParam
    JsonType.Array -> ArrayParam
    JsonType.Object -> ObjectParam
    JsonType.Null -> throw IllegalSchemaException("Cannot use a null value in a schema!")
}
