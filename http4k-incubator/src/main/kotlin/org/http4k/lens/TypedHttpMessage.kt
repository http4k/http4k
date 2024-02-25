package org.http4k.lens

import org.http4k.core.HttpMessage
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.UriTemplate
import org.http4k.routing.RequestWithRoute
import org.http4k.routing.RoutedRequest
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicReference
import kotlin.properties.ReadOnlyProperty
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

abstract class TypedHttpMessage {
    protected fun <T : Any, M : HttpMessage> required(spec: BiDiLensBuilder<M, T>) =
        object : ReadWriteProperty<M, T> {
            override fun getValue(thisRef: M, property: KProperty<*>) = spec.required(property.name)(thisRef)

            override fun setValue(thisRef: M, property: KProperty<*>, value: T) {
                spec.required(property.name)(value, thisRef)
            }
        }

    protected fun <T : Any, M : HttpMessage> optional(spec: BiDiLensBuilder<M, T>) =
        object : ReadWriteProperty<M, T?> {
            override fun getValue(thisRef: M, property: KProperty<*>) = spec.optional(property.name)(thisRef)

            override fun setValue(thisRef: M, property: KProperty<*>, value: T?) {
                spec.optional(property.name)(value, thisRef)
            }
        }

    protected fun <T : Any, M : HttpMessage> defaulted(spec: BiDiLensBuilder<M, T>, default: (M) -> T) =
        object : ReadWriteProperty<M, T> {
            override fun getValue(thisRef: M, property: KProperty<*>) = spec.defaulted(property.name, default)(thisRef)

            override fun setValue(thisRef: M, property: KProperty<*>, value: T) {
                spec.optional(property.name)(value, thisRef)
            }
        }

    protected fun <T : Any, M : HttpMessage> body(spec: BiDiBodyLensSpec<T>) = object : ReadWriteProperty<M, T> {
        override fun getValue(thisRef: M, property: KProperty<*>) = spec.toLens()(thisRef)

        override fun setValue(thisRef: M, property: KProperty<*>, value: T) {
            spec.toLens()(value, thisRef)
        }
    }
}

abstract class TypedRequest(request: Request) : TypedHttpMessage(), RequestWithRoute by httpMessage(when {
    request is RequestWithRoute -> request
    else -> RoutedRequest(request, UriTemplate.from(request.uri.path))
}) {
    protected constructor(method: Method, uri: Uri) : this(Request(method, uri))

    protected fun <T : Any> required(spec: PathLensSpec<T>) =
        ReadOnlyProperty<Request, T> { thisRef, property -> spec.of(property.name)(thisRef) }

    override fun toString() = super.toMessage()
}

open class TypedResponse(response: Response) : TypedHttpMessage(), Response by httpMessage(response) {
    protected constructor(status: Status) : this(Response(status))

    override fun toString() = super.toMessage()
}

private inline fun <reified T : HttpMessage> httpMessage(initial: T): T = Proxy.newProxyInstance(
    T::class.java.classLoader,
    arrayOf(T::class.java), object : InvocationHandler {
        private val ref = AtomicReference(initial)
        override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<out Any>?) =
            method(ref.get(), *(args ?: arrayOf<Any>()))
                .let {
                    when {
                        !method.returnType.isAssignableFrom(T::class.java) -> it
                        else -> proxy.apply { ref.set(it as T) }
                    }
                }
    }
) as T

