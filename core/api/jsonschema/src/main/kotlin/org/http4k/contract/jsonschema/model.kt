package org.http4k.contract.jsonschema

import org.http4k.contract.jsonschema.v3.FieldMetadata
import org.http4k.contract.jsonschema.v3.value
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.ArrayParam
import org.http4k.lens.ParamMeta.NullParam
import org.http4k.lens.ParamMeta.ObjectParam

class SchemaNode private constructor(
    private val name: String,
    private val paramMeta: ParamMeta,
    private val isNullable: Boolean,
    val example: Any?,
    metadata: FieldMetadata?,
    val definitions: Iterable<SchemaNode> = emptyList(),
    internal val arrayItem: ArrayItem
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

interface ArrayItems {
    fun definitions(): Iterable<SchemaNode>
}

sealed interface ArrayItem : ArrayItems {
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

internal object EmptyArray : ArrayItems {
    override fun definitions(): Iterable<SchemaNode> = emptyList()
}

internal class OneOfArray(private val schemas: Set<ArrayItem>) : ArrayItems {
    @Suppress("unused")
    val oneOf = schemas.toSet().sortedBy { it.javaClass.simpleName }

    override fun definitions() = schemas.flatMap { it.definitions() }
}

abstract class SchemaSortingMap(private val map: MutableMap<String, Any?>) : MutableMap<String, Any?> by map {
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

