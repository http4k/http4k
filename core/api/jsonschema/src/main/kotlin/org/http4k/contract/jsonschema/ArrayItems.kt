package org.http4k.contract.jsonschema

import org.http4k.contract.jsonschema.v3.value
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.ArrayParam
import org.http4k.lens.ParamMeta.NullParam

interface ArrayItems {
    fun definitions(): Iterable<SchemaNode>
}

sealed interface ArrayItem : ArrayItems {
    class Array(
        val items: ArrayItems, val format: Any?,
        @Transient
        private val definitions: Iterable<SchemaNode> = emptyList()
    ) : ArrayItem {
        @Suppress("unused")
        val type = ArrayParam(NullParam).value

        override fun definitions(): Iterable<SchemaNode> = definitions

        override fun equals(other: Any?): Boolean = when (other) {
            is Array -> this.items == other.items
            else -> false
        }

        override fun hashCode(): Int = items.hashCode()
    }

    class NonObject(
        paramMeta: ParamMeta, val format: Any?,
        @Transient
        private val definitions: Iterable<SchemaNode> = emptyList()
    ) : ArrayItem {
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
        @Transient
        private val definitions: Iterable<SchemaNode> = emptyList()
    ) : ArrayItem {
        override fun definitions(): Iterable<SchemaNode> = definitions
        override fun equals(other: Any?): Boolean = when (other) {
            is Ref -> this.`$ref` == other.`$ref`
            else -> false
        }

        override fun hashCode(): Int = `$ref`.hashCode()
    }
}

object EmptyArray : ArrayItems {
    override fun definitions(): Iterable<SchemaNode> = emptyList()
}

class OneOfArray(
    @Transient
    private val schemas: Set<ArrayItem> = emptySet()
) : ArrayItems {
    @Suppress("unused")
    val oneOf = schemas.toSet().sortedBy { it.javaClass.simpleName }

    override fun definitions() = schemas.flatMap { it.definitions() }
}
