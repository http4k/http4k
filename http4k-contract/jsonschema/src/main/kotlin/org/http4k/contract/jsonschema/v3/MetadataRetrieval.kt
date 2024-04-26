package org.http4k.contract.jsonschema.v3

import java.lang.IllegalArgumentException
import kotlin.reflect.KType
import kotlin.reflect.full.createType

fun interface MetadataRetrieval : (Any) -> FieldMetadata {
    companion object {
        fun compose(vararg retrieval: MetadataRetrieval) = MetadataRetrieval { target ->
            retrieval.asSequence().map { it(target) }.firstOrNull() ?: FieldMetadata() }
    }
}

class SimpleMetadataLookup(
    private val typeToMetadata: Map<KType, FieldMetadata>
) : MetadataRetrieval {
    override fun invoke(target: Any) = try {
        typeToMetadata[target::class.createType()] ?: FieldMetadata()
    } catch (e: IllegalArgumentException) {
        FieldMetadata()
    }
}
