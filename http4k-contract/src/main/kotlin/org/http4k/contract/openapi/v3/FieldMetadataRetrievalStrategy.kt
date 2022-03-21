package org.http4k.contract.openapi.v3

fun interface FieldMetadataRetrievalStrategy : (Any, String) -> FieldMetadata

class NoOpFieldMetadataRetrievalStrategy : FieldMetadataRetrievalStrategy {
    override fun invoke(target: Any, fieldName: String) = FieldMetadata.empty
}

/**
 * Combine Strategies, with the first taking precedence
 */
fun FieldMetadataRetrievalStrategy.then(that: FieldMetadataRetrievalStrategy) =
    FieldMetadataRetrievalStrategy { p1, p2 -> that(p1, p2) + this(p1, p2) }
