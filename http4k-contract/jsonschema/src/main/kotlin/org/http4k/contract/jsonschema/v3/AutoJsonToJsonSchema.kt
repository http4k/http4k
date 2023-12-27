package org.http4k.contract.jsonschema.v3

import org.http4k.contract.jsonschema.IllegalSchemaException
import org.http4k.contract.jsonschema.JsonSchema
import org.http4k.contract.jsonschema.JsonSchemaCreator
import org.http4k.contract.jsonschema.v3.SchemaModelNamer.Companion.Simple
import org.http4k.format.AutoMarshallingJson
import org.http4k.format.JsonType
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.ArrayParam
import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.IntegerParam
import org.http4k.lens.ParamMeta.NullParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.ObjectParam
import org.http4k.lens.ParamMeta.StringParam
import org.http4k.unquoted

class AutoJsonToJsonSchema<NODE : Any>(
    private val json: AutoMarshallingJson<NODE>,
    private val fieldRetrieval: FieldRetrieval = FieldRetrieval.compose(
        SimpleLookup(
            metadataRetrievalStrategy = PrimitivesFieldMetadataRetrievalStrategy
        )
    ),
    private val modelNamer: SchemaModelNamer = Simple,
    private val refLocationPrefix: String = "components/schemas"
) : JsonSchemaCreator<Any, NODE> {

    override fun toSchema(obj: Any, overrideDefinitionId: String?, refModelNamePrefix: String?): JsonSchema<NODE> {
        val schema = json.asJsonObject(obj).toSchema(obj, overrideDefinitionId, true, refModelNamePrefix.orEmpty(), null)
        return JsonSchema(
            json.asJsonObject(schema),
            schema.definitions().map { it.name() to json.asJsonObject(it) }.distinctBy { it.first }.toSet()
        )
    }

    private fun NODE.toSchema(
        value: Any,
        objName: String?,
        topLevel: Boolean,
        refModelNamePrefix: String,
        metadata: FieldMetadata?
    ) =
        when (val param = json.typeOf(this).toParam()) {
            is ArrayParam -> toArraySchema("", value, false, null, refModelNamePrefix)
            ObjectParam -> toObjectOrMapSchema(objName, value, false, topLevel, null, refModelNamePrefix)
            else -> value.javaClass.enumConstants?.let {
                toEnumSchema("", it[0], json.typeOf(this).toParam(), it, false, null)
            } ?: toSchema("", param, false, metadata)
        }

    private fun NODE.toSchema(name: String, paramMeta: ParamMeta, isNullable: Boolean, metadata: FieldMetadata?) =
        SchemaNode.Primitive(name, paramMeta, isNullable, this, metadata)

    private fun NODE.toArraySchema(
        name: String,
        obj: Any,
        isNullable: Boolean,
        metadata: FieldMetadata?,
        refModelNamePrefix: String
    ): SchemaNode.Array {
        val items = json.elements(this)
            .zip(items(obj)) { node: NODE, value: Any ->
                value.javaClass.enumConstants?.let {
                    node.toEnumSchema("", it[0], json.typeOf(node).toParam(), it, false, null)
                } ?: node.toSchema(
                    value,
                    null,
                    false,
                    refModelNamePrefix,
                    fieldRetrieval(FieldHolder(value), "value").metadata
                )
            }.map { it.arrayItem() }.toSet()

        val arrayItems = when (items.size) {
            0 -> EmptyArray
            1 -> items.first()
            else -> OneOfArray(items)
        }

        return SchemaNode.Array(name, isNullable, arrayItems, this, metadata)
    }

    private fun NODE.toEnumSchema(
        fieldName: String, obj: Any, param: ParamMeta,
        enumConstants: Array<Any>, isNullable: Boolean, metadata: FieldMetadata?
    ): SchemaNode =
        SchemaNode.Reference(
            fieldName,
            "#/$refLocationPrefix/${modelNamer(obj)}",
            SchemaNode.Enum(
                modelNamer(obj),
                param,
                isNullable,
                this,
                enumConstants.map { json.asFormatString(it).unquoted() },
                null
            ),
            metadata
        )

    private fun NODE.toObjectOrMapSchema(
        objName: String?,
        obj: Any,
        isNullable: Boolean,
        topLevel: Boolean,
        metadata: FieldMetadata?,
        refModelNamePrefix: String
    ) =
        if (obj is Map<*, *>) toMapSchema(objName, obj, isNullable, topLevel, metadata, refModelNamePrefix)
        else toObjectSchema(objName, obj, isNullable, topLevel, metadata, refModelNamePrefix)

    private fun NODE.toObjectSchema(
        objName: String?,
        obj: Any,
        isNullable: Boolean,
        topLevel: Boolean,
        metadata: FieldMetadata?,
        refModelNamePrefix: String
    ): SchemaNode.Reference {
        val properties = json.fields(this)
            .map { Triple(it.first, it.second, fieldRetrieval(obj, it.first)) }
            .map { (fieldName, field, kField) ->
                makePropertySchemaFor(
                    field,
                    fieldName,
                    kField.value,
                    kField.isNullable,
                    kField.metadata,
                    refModelNamePrefix
                )
            }.associateBy { it.name() }

        val nameToUseForRef = if (topLevel) objName ?: modelNamer(obj) else modelNamer(obj)

        return SchemaNode.Reference(
            objName
                ?: modelNamer(obj), "#/$refLocationPrefix/$refModelNamePrefix$nameToUseForRef",
            SchemaNode.Object(refModelNamePrefix + nameToUseForRef, isNullable, properties, this, null), metadata
        )
    }

    private fun NODE.toMapSchema(
        objName: String?,
        obj: Map<*, *>,
        isNullable: Boolean,
        topLevel: Boolean,
        metadata: FieldMetadata?,
        refModelNamePrefix: String
    ): SchemaNode {
        val objWithStringKeys = obj.mapKeys { it.key?.let(::toJsonKey) }
        val properties = json.fields(this)
            .map { Triple(it.first, it.second, objWithStringKeys[it.first]!!) }
            .map { (fieldName, field, value) ->
                makePropertySchemaFor(
                    field,
                    fieldName,
                    value,
                    true,
                    fieldRetrieval(FieldHolder(value), "value").metadata,
                    refModelNamePrefix
                )
            }
            .map { it.name() to it }.toMap()

        return if (topLevel && objName != null) {
            SchemaNode.Reference(
                objName, "#/$refLocationPrefix/$refModelNamePrefix$objName",
                SchemaNode.Object(refModelNamePrefix + objName, isNullable, properties, this, null), metadata
            )
        } else
            SchemaNode.MapType(
                objName ?: modelNamer(obj), isNullable,
                SchemaNode.Object(modelNamer(obj), isNullable, properties, this, null), metadata
            )
    }

    private fun makePropertySchemaFor(
        field: NODE,
        fieldName: String,
        value: Any,
        isNullable: Boolean,
        metadata: FieldMetadata?,
        refModelNamePrefix: String
    ) = when (val param = json.typeOf(field).toParam()) {
        is ArrayParam -> field.toArraySchema(fieldName, value, isNullable, metadata, refModelNamePrefix)
        ObjectParam -> field.toObjectOrMapSchema(fieldName, value, isNullable, false, metadata, refModelNamePrefix)
        else -> with(field) {
            value.javaClass.enumConstants
                ?.let { toEnumSchema(fieldName, value, param, it, isNullable, metadata) }
                ?: toSchema(fieldName, param, isNullable, metadata)
        }
    }

    private fun toJsonKey(it: Any): String {
        data class MapKey(val keyAsString: Any)
        return json.textValueOf(json.asJsonObject(MapKey(it)), "keyAsString")!!
    }
}

fun interface SchemaModelNamer : (Any) -> String {
    companion object {
        val Simple: SchemaModelNamer = SchemaModelNamer { it.javaClass.simpleName }
        val Full: SchemaModelNamer = SchemaModelNamer { it.javaClass.name }
    }
}

private interface ArrayItems {
    fun definitions(): Iterable<SchemaNode>
}

private sealed class ArrayItem : ArrayItems {
    class Array(val items: ArrayItems, private val schema: SchemaNode) : ArrayItem() {
        val type = ArrayParam(NullParam).value
        val format = schema.format

        override fun definitions(): Iterable<SchemaNode> = schema.definitions()

        override fun equals(other: Any?): Boolean = when (other) {
            is Array -> this.items == other.items
            else -> false
        }

        override fun hashCode(): Int = items.hashCode()
    }

    class NonObject(paramMeta: ParamMeta, private val schema: SchemaNode) : ArrayItem() {
        val type = paramMeta.value
        val format = schema.format

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as NonObject

            if (type != other.type) return false

            return true
        }

        override fun hashCode(): Int = type.hashCode()
        override fun definitions(): Iterable<SchemaNode> = schema.definitions()
    }

    class Ref(val `$ref`: String, private val schema: SchemaNode) : ArrayItem() {
        override fun definitions(): Iterable<SchemaNode> = schema.definitions()
        override fun equals(other: Any?): Boolean = when (other) {
            is Ref -> this.`$ref` == other.`$ref`
            else -> false
        }

        override fun hashCode(): Int = `$ref`.hashCode()
    }
}

private object EmptyArray : ArrayItems {
    override fun definitions(): Iterable<SchemaNode> = emptyList()
}

private class OneOfArray(private val schemas: Set<ArrayItem>) : ArrayItems {
    val oneOf = schemas.toSet().sortedBy { it.javaClass.simpleName }

    override fun definitions() = schemas.flatMap { it.definitions() }
}

private sealed class SchemaNode(
    private val _name: String,
    private val _paramMeta: ParamMeta,
    private val isNullable: Boolean,
    val example: Any?,
    metadata: FieldMetadata?
) {
    abstract fun definitions(): Iterable<SchemaNode>

    fun name() = _name

    fun paramMeta() = _paramMeta
    abstract fun arrayItem(): ArrayItem

    val description = metadata?.extra?.get("description")
    val format = metadata?.extra?.get("format")
    val default = metadata?.extra?.get("default")
    val title = metadata?.extra?.get("title")
    val multipleOf = metadata?.extra?.get("multipleOf")
    val maximum = metadata?.extra?.get("maximum")
    val exclusiveMaximum = metadata?.extra?.get("exclusiveMaximum")
    val minimum = metadata?.extra?.get("minimum")
    val exclusiveMinimum = metadata?.extra?.get("exclusiveMinimum")
    val maxLength = metadata?.extra?.get("maxLength")
    val minLength = metadata?.extra?.get("minLength")
    val pattern = metadata?.extra?.get("pattern")
    val maxItems = metadata?.extra?.get("maxItems")
    val minItems = metadata?.extra?.get("minItems")
    val uniqueItems = metadata?.extra?.get("uniqueItems")
    val maxProperties = metadata?.extra?.get("maxProperties")
    val minProperties = metadata?.extra?.get("minProperties")

    class Primitive(name: String, paramMeta: ParamMeta, isNullable: Boolean, example: Any?, metadata: FieldMetadata?) :
        SchemaNode(name, paramMeta, isNullable, example, metadata) {
        val type = paramMeta().value
        override fun arrayItem() = ArrayItem.NonObject(paramMeta(), this)
        override fun definitions() = emptyList<SchemaNode>()
    }

    class Enum(
        name: String,
        paramMeta: ParamMeta,
        isNullable: Boolean,
        example: Any?,
        val enum: List<String>,
        metadata: FieldMetadata?
    ) :
        SchemaNode(name, paramMeta, isNullable, example, metadata) {
        val type = paramMeta().value
        override fun arrayItem() = ArrayItem.Ref(name(), this)
        override fun definitions() = emptyList<SchemaNode>()
    }

    class Array(name: String, isNullable: Boolean, val items: ArrayItems, example: Any?, metadata: FieldMetadata?) :
        SchemaNode(
            name,
            ArrayParam(items.definitions().map { it.paramMeta() }.toSet().firstOrNull() ?: NullParam),
            isNullable,
            example,
            metadata
        ) {
        val type = paramMeta().value

        override fun arrayItem() = when (paramMeta()) {
            is ArrayParam -> ArrayItem.Array(items, this)
            ObjectParam -> ArrayItem.Ref(name(), this)
            else -> ArrayItem.NonObject(paramMeta(), this)
        }

        override fun definitions() = items.definitions()
    }

    class Object(
        name: String, isNullable: Boolean, val properties: Map<String, SchemaNode>,
        example: Any?, metadata: FieldMetadata?
    ) : SchemaNode(name, ObjectParam, isNullable, example, metadata) {
        val type = paramMeta().value
        val required =
            properties.let { it.filterNot { it.value.isNullable }.takeIf { it.isNotEmpty() }?.keys?.sorted() }

        override fun arrayItem() = ArrayItem.Ref(name(), this)
        override fun definitions() = properties.values.flatMap { it.definitions() }
    }

    class Reference(
        name: String,
        val `$ref`: String,
        private val schemaNode: SchemaNode,
        metadata: FieldMetadata?
    ) : SchemaNode(name, ObjectParam, schemaNode.isNullable, null, metadata) {
        override fun arrayItem() = ArrayItem.Ref(`$ref`, this)
        override fun definitions() = listOf(schemaNode) + schemaNode.definitions()
    }

    class MapType(name: String, isNullable: Boolean, val additionalProperties: SchemaNode, metadata: FieldMetadata?) :
        SchemaNode(name, ObjectParam, isNullable, null, metadata) {
        val type = paramMeta().value
        override fun arrayItem() = ArrayItem.Ref(name(), this)
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
    JsonType.Array -> ArrayParam(NullParam)
    JsonType.Object -> ObjectParam
    JsonType.Null -> throw IllegalSchemaException("Cannot use a null value in a schema!")
}

data class FieldHolder(@JvmField val value: Any)
