package org.http4k.lens

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.lens.TypedField.Body
import org.http4k.lens.TypedField.Defaulted
import org.http4k.lens.TypedField.Optional
import org.http4k.lens.TypedField.Required
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

data class MetaAndExample<OUT>(val meta: Meta, val example: OUT?)

fun <IN : Any> KClass<IN>.metaAndExample() = declaredMemberProperties.toList()
    .flatMap {
        it.isAccessible = true
        with(it.getDelegate(primaryConstructor!!.call(Request(GET, "")))) {
            when (this) {
                is Body<*, *> -> spec.toLens().metas.map { MetaAndExample(it, example) }
                is Defaulted<*, *> -> listOf(MetaAndExample(spec.required(it.name).meta, null))
                is Optional<*, *> -> listOf(MetaAndExample(spec.optional(it.name).meta, null))
                is Required<*, *> -> listOf(MetaAndExample(spec.required(it.name).meta, null))
                is TypedField.Path<*> -> listOf(MetaAndExample(spec.of(it.name).meta, null))
                else -> emptyList()
            }
        }
    }
