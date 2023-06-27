package org.http4k.contract.jsonschema.v3

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
    override fun invoke(target: Any, fieldName: String): FieldMetadata =
        when (target::class.declaredMembers.find { it.name == fieldName }?.call(target)) {
            is Int -> FieldMetadata("format" to "int32")
            is Long -> FieldMetadata("format" to "int64")
            is Double -> FieldMetadata("format" to "double")
            is Float -> FieldMetadata("format" to "float")
            is Instant -> FieldMetadata("format" to "date-time")
            is LocalDate -> FieldMetadata("format" to "date")
            is UUID -> FieldMetadata("format" to "uuid")
            is Uri -> FieldMetadata("format" to "uri")
            else -> FieldMetadata()
        }
}
