package org.http4k.contract.openapi.v3

import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.javaGetter

interface FieldRetrieval : (Any, String) -> Field {
    companion object {
        fun compose(vararg retrieval: FieldRetrieval) = object : FieldRetrieval {
            override fun invoke(target: Any, name: String): Field =
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

class SimpleLookup(private val renamingStrategy: (String) -> String = { it }) : FieldRetrieval {
    override fun invoke(target: Any, name: String): Field {
        val fields = try {
            target::class.memberProperties.map { renamingStrategy(it.name) to it }.toMap()
        } catch (e: Error) {
            emptyMap<String, KProperty1<out Any, Any?>>()
        }

        return fields[name]
            ?.let { field ->
                field.javaGetter
                    ?.let { it(target) }
                    ?.let { it to field.returnType.isMarkedNullable }
            }
            ?.let { Field(it.first, it.second) } ?: throw NoFieldFound(name, target)
    }
}

class NoFieldFound(name: String, target: Any, cause: Throwable? = null) : RuntimeException("Could not find $name in $target", cause)

data class Field(val value: Any, val isNullable: Boolean)
