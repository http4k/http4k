package org.http4k.contract.jsonschema.v3

import dev.forkhandles.data.DataContainer
import dev.forkhandles.data.Metadatum

/**
 * Values of the json schema metadata
 */
sealed interface Data4kJsonSchemaMeta {
    data object example : Data4kJsonSchemaMeta
    data object description : Data4kJsonSchemaMeta
    data object format : Data4kJsonSchemaMeta
    data object default : Data4kJsonSchemaMeta
    data object title : Data4kJsonSchemaMeta
    data object multipleOf : Data4kJsonSchemaMeta
    data object maximum : Data4kJsonSchemaMeta
    data object exclusiveMaximum : Data4kJsonSchemaMeta
    data object minimum : Data4kJsonSchemaMeta
    data object exclusiveMinimum : Data4kJsonSchemaMeta
    data object maxLength : Data4kJsonSchemaMeta
    data object minLength : Data4kJsonSchemaMeta
    data object pattern : Data4kJsonSchemaMeta
    data object maxItems : Data4kJsonSchemaMeta
    data object minItems : Data4kJsonSchemaMeta
    data object uniqueItems : Data4kJsonSchemaMeta
    data object maxProperties : Data4kJsonSchemaMeta
    data object minProperties : Data4kJsonSchemaMeta

    class Datum<T>(val name: String, val value: T) : Metadatum

    infix fun of(t: Any) = Datum(this::class.simpleName ?: "error", t)
}

object Data4kFieldMetadataRetrievalStrategy : FieldMetadataRetrievalStrategy {
    override fun invoke(target: Any, fieldName: String) = FieldMetadata(
        when (target) {
            is DataContainer<*> -> target.propertyMetadata()
                .firstOrNull { it.name == fieldName }
                ?.let {
                    it.data.filterIsInstance<Data4kJsonSchemaMeta.Datum<*>>()
                        .map { it.name to it.value }
                }
                ?.toMap() ?: emptyMap()
            else -> emptyMap()
        }
    )
}
