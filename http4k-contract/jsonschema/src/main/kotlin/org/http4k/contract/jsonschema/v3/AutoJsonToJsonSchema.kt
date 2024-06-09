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
    private val refLocationPrefix: String = "components/schemas",
    private val metadataRetrieval: MetadataRetrieval = MetadataRetrieval.compose(SimpleMetadataLookup(emptyMap()))
) : JsonSchemaCreator<Any, NODE> {

    override fun toSchema(obj: Any, overrideDefinitionId: String?, refModelNamePrefix: String?): JsonSchema<NODE> {
        val schema =
            json.asJsonObject(obj).toSchema(obj, overrideDefinitionId, true, refModelNamePrefix.orEmpty(), metadataRetrieval(obj))
        return JsonSchema(
            json.asJsonObject(schema),
            schema.definitions.map { it.name() to json.asJsonObject(it) }.distinctBy { it.first }.toSet()
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
            ObjectParam -> toObjectOrMapSchema(objName, value, false, topLevel, metadata, refModelNamePrefix)
            else -> value.javaClass.enumConstants?.let {
                toEnumSchema("", it[0], json.typeOf(this).toParam(), it, false, null, refModelNamePrefix)
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
    ): SchemaNode {
        val items = json.elements(this)
            .zip(items(obj)) { node: NODE, value: Any ->
                value.javaClass.enumConstants?.let {
                    node.toEnumSchema("", it[0], json.typeOf(node).toParam(), it, false, null, refModelNamePrefix)
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
        enumConstants: Array<Any>, isNullable: Boolean, metadata: FieldMetadata?,
        refModelNamePrefix: String
    ): SchemaNode =
        SchemaNode.Reference(
            fieldName,
            "#/$refLocationPrefix/$refModelNamePrefix${modelNamer(obj)}",
            SchemaNode.Enum(
                "$refModelNamePrefix${modelNamer(obj)}",
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
    ): SchemaNode {
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
            SchemaNode.Object(refModelNamePrefix + nameToUseForRef, isNullable, properties, this, metadata), null
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
                ?.let { toEnumSchema(fieldName, value, param, it, isNullable, metadata, refModelNamePrefix) }
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
        val Canonical: SchemaModelNamer = SchemaModelNamer { it.javaClass.canonicalName }
    }
}

private interface ArrayItems {
    fun definitions(): Iterable<SchemaNode>
}

private sealed interface ArrayItem : ArrayItems {
    class Array(val items: ArrayItems, val format: Any?, private val definitions: Iterable<SchemaNode>) : ArrayItem {
        @Suppress("unused")
        val type = ArrayParam(NullParam).value

        override fun definitions(): Iterable<SchemaNode> = definitions

        override fun equals(other: Any?): Boolean = when (other) {
            is Array -> this.items == other.items
            else -> false
        }

        override fun hashCode(): Int = items.hashCode()
    }

    class NonObject(paramMeta: ParamMeta, val format: Any?, private val definitions: Iterable<SchemaNode>) : ArrayItem {
        val type = paramMeta.value

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as NonObject

            return type == other.type
        }

        override fun hashCode(): Int = type.hashCode()
        override fun definitions(): Iterable<SchemaNode> = definitions
    }

    class Ref(
        @Suppress("unused")
        val `$ref`: String,
        private val definitions: Iterable<SchemaNode>
    ) : ArrayItem {
        override fun definitions(): Iterable<SchemaNode> = definitions
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
    @Suppress("unused")
    val oneOf = schemas.toSet().sortedBy { it.javaClass.simpleName }

    override fun definitions() = schemas.flatMap { it.definitions() }
}

private abstract class SchemaSortingMap(private val map: MutableMap<String, Any?>) : MutableMap<String, Any?> by map {
    override val entries
        get() = map.toSortedMap(compareBy<String> { sortOrder(it) }.thenBy { it }).entries

    private fun sortOrder(o1: String) = SORT_ORDER.indexOf(o1).let {
        if (it > -1) it else Int.MAX_VALUE
    }

    companion object {
        val SORT_ORDER = listOf(
            "properties",
            "items",
            "\$ref",
            "example",
            "enum",
            "additionalProperties",
            "description",
            "format",
            "default",
            "multipleOf",
            "maximum",
            "exclusiveMaximum",
            "minimum",
            "exclusiveMinimum",
            "maxLength",
            "minLength",
            "pattern",
            "maxItems",
            "minItems",
            "uniqueItems",
            "maxProperties",
            "minProperties",
            "type",
            "required",
            "title",
        )
    }
}

private class SchemaNode(
    private val name: String,
    private val paramMeta: ParamMeta,
    private val isNullable: Boolean,
    val example: Any?,
    metadata: FieldMetadata?,
    val definitions: Iterable<SchemaNode> = emptyList(),
    val arrayItem: ArrayItem
) : SchemaSortingMap(metadata?.extra?.toMutableMap() ?: mutableMapOf()) {
    init {
        this["format"] = this["format"]
        this["example"] = example
    }

    fun name() = name
    fun arrayItem(): ArrayItem = arrayItem

    companion object {
        fun Primitive(
            name: String,
            paramMeta: ParamMeta,
            isNullable: Boolean,
            example: Any?,
            metadata: FieldMetadata?
        ) =
            SchemaNode(
                name = name,
                paramMeta = paramMeta,
                isNullable = isNullable,
                example = example,
                metadata = metadata,
                arrayItem = ArrayItem.NonObject(
                    paramMeta,
                    metadata.format(), emptyList()
                )
            ).apply {
                this["type"] = paramMeta.value
                this["nullable"] = isNullable
            }

        fun Enum(
            name: String,
            paramMeta: ParamMeta,
            isNullable: Boolean,
            example: Any?,
            enum: List<String>,
            metadata: FieldMetadata?
        ) =
            SchemaNode(
                name = name,
                paramMeta = paramMeta,
                isNullable = isNullable,
                example = example,
                metadata = metadata,
                arrayItem = ArrayItem.Ref(name, emptyList())
            ).apply {
                this["type"] = paramMeta.value
                this["nullable"] = isNullable
                this["enum"] = enum
            }

        fun Array(
            name: String,
            isNullable: Boolean,
            items: ArrayItems,
            example: Any?,
            metadata: FieldMetadata?
        ): SchemaNode {
            val paramMeta: ParamMeta =
                ArrayParam(items.definitions().map { it.paramMeta }.toSet().firstOrNull() ?: NullParam)
            return SchemaNode(
                name = name,
                paramMeta = paramMeta,
                isNullable = isNullable,
                example = example,
                metadata = metadata,
                definitions = items.definitions(),
                arrayItem = ArrayItem.Array(items, metadata.format(), items.definitions())
            ).apply {
                this["type"] = paramMeta.value
                this["nullable"] = isNullable
                this["items"] = items
            }
        }

        private fun FieldMetadata?.format() = this?.extra?.get("format")

        fun Object(
            name: String, isNullable: Boolean, properties: Map<String, SchemaNode>,
            example: Any?, metadata: FieldMetadata?
        ): SchemaNode {
            val paramMeta = ObjectParam
            return SchemaNode(
                name = name,
                paramMeta = paramMeta,
                isNullable = isNullable,
                example = example,
                metadata = metadata,
                definitions = properties.values.flatMap { it.definitions },
                arrayItem = ArrayItem.Ref(name, properties.values.flatMap { it.definitions })
            ).apply {
                this["type"] = paramMeta.value
                this["required"] =
                    properties.let { it.filterNot { it.value.isNullable }.takeIf { it.isNotEmpty() }?.keys?.sorted() }
                this["properties"] = properties
            }
        }

        fun Reference(
            name: String,
            ref: String,
            schemaNode: SchemaNode,
            metadata: FieldMetadata?
        ) = SchemaNode(
            name = name,
            paramMeta = ObjectParam,
            isNullable = schemaNode.isNullable,
            example = null,
            metadata = metadata,
            definitions = listOf(schemaNode) + schemaNode.definitions,
            arrayItem = ArrayItem.Ref(ref, listOf(schemaNode) + schemaNode.definitions)
        ).apply {
            this["\$ref"] = ref
        }

        fun MapType(
            name: String,
            isNullable: Boolean,
            additionalProperties: SchemaNode,
            metadata: FieldMetadata?
        ): SchemaNode {
            val paramMeta = ObjectParam
            return SchemaNode(
                name,
                paramMeta,
                isNullable,
                null,
                metadata,
                definitions = additionalProperties.definitions,
                arrayItem = ArrayItem.Ref(name, additionalProperties.definitions)
            ).apply {
                this["type"] = paramMeta.value
                this["additionalProperties"] = additionalProperties
            }
        }
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
