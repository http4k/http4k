package org.http4k.contract.openapi.v3

import dev.forkhandles.values.Value
import org.http4k.core.Uri
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

/**
 * Set format values for OpenApi descriptions for fields of this type
 */
object Values4kFieldMetadataRetrievalStrategy : FieldMetadataRetrievalStrategy {
    override fun invoke(target: Any, fieldName: String) =
        when {
            target.isAValue<Int>() -> FieldMetadata("format" to "int32")
            target.isAValue<Long>() -> FieldMetadata("format" to "int64")
            target.isAValue<Double>() -> FieldMetadata("format" to "double")
            target.isAValue<Float>() -> FieldMetadata("format" to "float")

            target.isAValue<Instant>() -> FieldMetadata("format" to "date-time")
            target.isAValue<LocalDate>() -> FieldMetadata("format" to "date")

            target.isAValue<UUID>() -> FieldMetadata("format" to "uuid")
            target.isAValue<Uri>() -> FieldMetadata("format" to "uri")

            else -> FieldMetadata()
        }
}

private inline fun <reified T> Any.isAValue() =
    (this as? Value<*>)?.value?.javaClass == T::class.java
