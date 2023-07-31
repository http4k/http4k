package org.http4k.contract.jsonschema.v3

import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaField
import kotlin.reflect.jvm.javaGetter

fun interface FieldRetrieval : (Any, String) -> Field {
    companion object {
        fun compose(vararg retrieval: FieldRetrieval) = FieldRetrieval { target, name ->
            retrieval.asSequence().mapNotNull {
                try {
                    it(target, name)
                } catch (e: NoFieldFound) {
                    null
                }
            }.firstOrNull() ?: throw NoFieldFound(name, target)
        }
    }
}

class SimpleLookup(
    private val renamingStrategy: (String) -> String = { it },
    private val metadataRetrievalStrategy: FieldMetadataRetrievalStrategy = NoOpFieldMetadataRetrievalStrategy()
) : FieldRetrieval {
    override fun invoke(target: Any, name: String): Field {
        val fields = try {
            target::class.memberProperties.associateBy { renamingStrategy(it.name) }
        } catch (e: Error) {
            emptyMap<String, KProperty1<out Any, Any?>>()
        }

        return fields[name]
            ?.let { field ->
                field.javaGetter
                    ?.let { it(target) }
                    ?.let { it to field.returnType.isMarkedNullable }
                    ?: fields[name]?.javaField?.takeIf { it.trySetAccessible() }?.get(target)?.let { it to true }
            }
            ?.let { Field(it.first, it.second, metadataRetrievalStrategy(target, name)) } ?: throw NoFieldFound(
            name,
            target
        )
    }
}

data class FieldMetadata(val extra: Map<String, Any?> = emptyMap()) {
    constructor(vararg pairs: Pair<String, Any?>) : this(pairs.toMap())

    operator fun plus(that: FieldMetadata) = FieldMetadata(extra + that.extra)

    companion object {
        val empty: FieldMetadata = FieldMetadata()
    }
}

class NoFieldFound(name: String, target: Any, cause: Throwable? = null) :
    RuntimeException("Could not find $name in $target", cause)

data class Field(val value: Any, val isNullable: Boolean, val metadata: FieldMetadata)
