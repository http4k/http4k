package org.http4k.lens

import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.TypedField.Body
import org.http4k.lens.TypedField.Defaulted
import org.http4k.lens.TypedField.Optional
import org.http4k.lens.TypedField.Required
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

fun <IN : TypedHttpMessage> KClass<IN>.routeParams() = declaredMemberProperties.toList()
    .flatMap {
        it.isAccessible = true
        with(it.getDelegate(delegate())) {
            when (this) {
                is Body<*, *> -> spec.toLens().metas.map { RouteParam.Body(it, example) }
                is Defaulted<*, *> -> listOf(RouteParam.NonBody(spec.required(it.name).meta))
                is Optional<*, *> -> listOf(RouteParam.NonBody(spec.optional(it.name).meta))
                is Required<*, *> -> listOf(RouteParam.NonBody(spec.required(it.name).meta))
                is TypedField.Path<*> -> listOf(RouteParam.NonBody(spec.of(it.name).meta))
                else -> emptyList()
            }
        }
    }

private fun <IN : Any> KClass<IN>.delegate() = try {
    primaryConstructor!!.call(Request(GET, ""))
} catch (e: IllegalArgumentException) {
    primaryConstructor!!.call(Response(OK))
}
