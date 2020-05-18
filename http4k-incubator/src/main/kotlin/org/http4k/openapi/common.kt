package org.http4k.openapi

import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import com.squareup.kotlinpoet.asClassName
import com.squareup.kotlinpoet.asTypeName

sealed class NamedSchema(name: String) {
    val fieldName = name.clean().decapitalize()

    data class Existing(val name: String, val typeName: TypeName) : NamedSchema(name)
    data class Generated(val name: String, val schema: SchemaSpec) : NamedSchema(name)
}

fun String.clean() = filter { it.isLetterOrDigit() }

fun SchemaSpec.namedSchema(modelName: String): NamedSchema = when (this) {
    is SchemaSpec.RefSpec -> NamedSchema.Generated(schemaName, this)
    is SchemaSpec.ArraySpec -> when (val itemSchema = itemsSpec().namedSchema(modelName)) {
        is NamedSchema.Generated -> itemSchema
        is NamedSchema.Existing -> NamedSchema.Existing(modelName, List::class.asTypeName().parameterizedBy(itemSchema.typeName))
    }
    else -> clazz?.let { NamedSchema.Existing(modelName, it.asClassName()) }
        ?: NamedSchema.Generated(modelName, this)
}
