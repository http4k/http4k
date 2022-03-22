package org.http4k.contract.openapi.v3

import org.http4k.core.Uri
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.reflect.full.declaredMembers

fun interface FieldMetadataRetrievalStrategy : (Any, String) -> FieldMetadata

class NoOpFieldMetadataRetrievalStrategy : FieldMetadataRetrievalStrategy {
    override fun invoke(target: Any, fieldName: String) = FieldMetadata.empty
}

/**
 * Combine Strategies, with the first taking precedence
 */
fun FieldMetadataRetrievalStrategy.then(that: FieldMetadataRetrievalStrategy) =
    FieldMetadataRetrievalStrategy { p1, p2 -> that(p1, p2) + this(p1, p2) }

object PrimitivesFieldMetadataRetrievalStrategy : FieldMetadataRetrievalStrategy {
    override fun invoke(target: Any, fieldName: String): FieldMetadata {
        val value = target::class.declaredMembers.find { it.name == fieldName }?.call(target)

        return when {
            value is Int -> FieldMetadata("format" to "int32")
            value is Long -> FieldMetadata("format" to "int64")
            value is Double -> FieldMetadata("format" to "double")
            value is Float -> FieldMetadata("format" to "float")

            value is Instant -> FieldMetadata("format" to "date-time")
            value is LocalDate -> FieldMetadata("format" to "date")

            value is UUID -> FieldMetadata("format" to "uuid")
            value is Uri -> FieldMetadata("format" to "uri")

            else -> FieldMetadata()
        }
    }
}
