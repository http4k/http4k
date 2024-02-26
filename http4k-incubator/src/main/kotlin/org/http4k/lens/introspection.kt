package org.http4k.lens

import org.http4k.core.Method
import org.http4k.core.Request
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

fun <T : Any> KClass<T>.metas() = declaredMemberProperties.toList()
    .flatMap {
        it.isAccessible = true
        when (val delegate = it.getDelegate(primaryConstructor!!.call(Request(Method.GET, "")))) {
            is TypedField.Body<*, *> -> delegate.spec.toLens().metas
            is TypedField.Defaulted<*, *> -> listOf(delegate.spec.required(it.name).meta)
            is TypedField.Optional<*, *> -> listOf(delegate.spec.optional(it.name).meta)
            is TypedField.Required<*, *> -> listOf(delegate.spec.required(it.name).meta)
            is TypedField.Path<*> -> listOf(delegate.spec.of(it.name).meta)
            else -> emptyList()
        }
    }
