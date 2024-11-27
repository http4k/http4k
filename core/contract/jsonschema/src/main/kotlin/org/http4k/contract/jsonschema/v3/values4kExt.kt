package org.http4k.contract.jsonschema.v3

import dev.forkhandles.values.Value
import org.http4k.core.Uri
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.full.declaredMembers

/**
 * Set format values for OpenApi descriptions for fields of this type
 */
object Values4kFieldMetadataRetrievalStrategy : FieldMetadataRetrievalStrategy {
    override fun invoke(target: Any, fieldName: String): FieldMetadata {
        val value = target::class.declaredMembers.find { it.name == fieldName }?.call(target)

        return when {
            value.isAValue<Int>() -> FieldMetadata("format" to "int32")
            value.isAValue<Long>() -> FieldMetadata("format" to "int64")
            value.isAValue<Double>() -> FieldMetadata("format" to "double")
            value.isAValue<Float>() -> FieldMetadata("format" to "float")

            value.isAValue<Instant>() -> FieldMetadata("format" to "date-time")
            value.isAValue<LocalDate>() -> FieldMetadata("format" to "date")

            value.isAValue<UUID>() -> FieldMetadata("format" to "uuid")
            value.isAValue<Uri>() -> FieldMetadata("format" to "uri")

            else -> FieldMetadata()
        }
    }
}

private inline fun <reified T> Any?.isAValue() = (this as? Value<*>)?.value?.javaClass == T::class.java
