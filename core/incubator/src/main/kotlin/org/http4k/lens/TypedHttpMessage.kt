package org.http4k.lens

import org.http4k.core.HttpMessage
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.UriTemplate
import org.http4k.lens.TypedField.Body
import org.http4k.lens.TypedField.Defaulted
import org.http4k.lens.TypedField.Optional
import org.http4k.lens.TypedField.Path
import org.http4k.lens.TypedField.Required
import org.http4k.routing.RoutedRequest
import java.lang.reflect.InvocationHandler
import java.lang.reflect.Proxy
import java.util.concurrent.atomic.AtomicReference

abstract class TypedHttpMessage {
    protected fun <IN : HttpMessage, OUT : Any> required(spec: BiDiLensBuilder<IN, OUT>) =
        Required(spec)

    protected fun <IN : HttpMessage, OUT : Any> optional(spec: BiDiLensBuilder<IN, OUT>) =
        Optional(spec)

    protected fun <IN : HttpMessage, OUT : Any> defaulted(
        spec: BiDiLensBuilder<IN, OUT>,
        default: (IN) -> OUT
    ) =
        Defaulted(spec, default)

    protected fun <IN : HttpMessage, OUT : Any> body(spec: BiDiBodyLensSpec<OUT>, example: OUT? = null) =
        Body<IN, OUT>(spec, example)
}

abstract class TypedRequest(private val request: Request) : TypedHttpMessage(), Request by httpMessage<Request>(
    when {
        request is RoutedRequest -> request
        else -> RoutedRequest(request, UriTemplate.from(request.uri.path))
    }
) {
    protected constructor(method: Method, uri: Uri) : this(Request(method, uri))

    protected fun <OUT : Any> required(spec: PathLensSpec<OUT>): Path<OUT> = Path(spec)

    override fun toString() = request.toMessage()
}

abstract class TypedResponse(private val response: Response) : TypedHttpMessage(), Response by httpMessage(response) {
    protected constructor(status: Status) : this(Response(status))

    override fun toString() = response.toMessage()
}

private inline fun <reified IN> httpMessage(initial: IN): IN = Proxy.newProxyInstance(
    IN::class.java.classLoader,
    arrayOf(IN::class.java), object : InvocationHandler {
        private val ref = AtomicReference(initial)
        override fun invoke(proxy: Any, method: java.lang.reflect.Method, args: Array<out Any>?) =
            method(ref.get(), *(args ?: arrayOf<Any>()))
                .let {
                    when {
                        !method.returnType.isAssignableFrom(IN::class.java) -> it
                        else -> proxy.apply { ref.set(it as IN) }
                    }
                }
    }
) as IN
