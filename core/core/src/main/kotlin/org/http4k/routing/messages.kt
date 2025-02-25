package org.http4k.routing

import org.http4k.core.Body
import org.http4k.core.Headers
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.UriTemplate
import org.http4k.lens.RequestKey
import org.http4k.routing.RoutedMessage.Companion.X_URI_TEMPLATE
import java.io.InputStream



@Deprecated("Use RequestWithContext or ResponseWithContext instead", ReplaceWith("RequestWithContext"))
interface RoutedMessage {

    companion object {
        const val X_URI_TEMPLATE = "xUriTemplate"
         val uriTemplateLensRequestKey = RequestKey.optional<UriTemplate>(X_URI_TEMPLATE)
    }
}

fun Request.uriTemplate(): UriTemplate? = (this as? RequestWithContext)?.context?.get(X_URI_TEMPLATE) as? UriTemplate
fun Response.uriTemplate(): UriTemplate? = (this as? ResponseWithContext)?.context?.get(X_URI_TEMPLATE) as? UriTemplate

fun Request.uriTemplate(uriTemplate: UriTemplate) = when(this){
        is RequestWithContext -> RequestWithContext(delegate, context + (X_URI_TEMPLATE to uriTemplate))
        else -> RequestWithContext(this, mapOf(X_URI_TEMPLATE to uriTemplate))
}
fun Response.uriTemplate(uriTemplate: UriTemplate) = when(this){
    is ResponseWithContext -> ResponseWithContext(delegate, context + (X_URI_TEMPLATE to uriTemplate))
    else -> ResponseWithContext(this, mapOf(X_URI_TEMPLATE to uriTemplate))
}

data class RequestWithContext(val delegate: Request, val context: Map<String, Any?> = emptyMap()) :
    Request by delegate {

    override fun equals(other: Any?): Boolean = delegate == other

    override fun hashCode(): Int = delegate.hashCode()

    override fun toString(): String = delegate.toString()

    override fun method(method: Method): Request = RequestWithContext(delegate.method(method), context)

    override fun uri(uri: Uri): Request = RequestWithContext(delegate.uri(uri), context)

    override fun query(name: String, value: String?): Request = RequestWithContext(delegate.query(name, value), context)

    override fun removeQuery(name: String): Request = RequestWithContext(delegate.removeQuery(name), context)

    override fun removeQueries(prefix: String): Request = RequestWithContext(delegate.removeQueries(prefix), context)

    override fun source(source: RequestSource): Request = RequestWithContext(delegate.source(source), context)

    override fun header(name: String, value: String?): Request =
        RequestWithContext(delegate.header(name, value), context)

    override fun headers(headers: Headers): Request = RequestWithContext(delegate.headers(headers), context)

    override fun replaceHeader(name: String, value: String?): Request =
        RequestWithContext(delegate.replaceHeader(name, value), context)

    override fun replaceHeaders(source: Headers): Request = RequestWithContext(delegate.replaceHeaders(source), context)

    override fun removeHeader(name: String): Request = RequestWithContext(delegate.removeHeader(name), context)

    override fun removeHeaders(prefix: String): Request = RequestWithContext(delegate.removeHeaders(prefix), context)

    override fun body(body: Body): Request = RequestWithContext(delegate.body(body), context)

    override fun body(body: String): Request = RequestWithContext(delegate.body(body), context)

    override fun body(body: InputStream, length: Long?): Request =
        RequestWithContext(delegate.body(body, length), context)
}

data class ResponseWithContext(val delegate: Response, val context: Map<String, Any> = emptyMap()) :
    Response by delegate {

    override fun equals(other: Any?): Boolean = delegate == other

    override fun hashCode(): Int = delegate.hashCode()

    override fun toString(): String = delegate.toString()

    override fun header(name: String, value: String?): Response =
        ResponseWithContext(delegate.header(name, value), context)

    override fun headers(headers: Headers): Response = ResponseWithContext(delegate.headers(headers), context)

    override fun replaceHeader(name: String, value: String?): Response =
        ResponseWithContext(delegate.replaceHeader(name, value), context)

    override fun replaceHeaders(source: Headers): Response =
        ResponseWithContext(delegate.replaceHeaders(source), context)

    override fun removeHeader(name: String): Response = ResponseWithContext(delegate.removeHeader(name), context)

    override fun removeHeaders(prefix: String): Response = ResponseWithContext(delegate.removeHeaders(prefix), context)

    override fun body(body: Body): Response = ResponseWithContext(delegate.body(body), context)

    override fun body(body: String): Response = ResponseWithContext(delegate.body(body), context)

    override fun body(body: InputStream, length: Long?): Response =
        ResponseWithContext(delegate.body(body, length), context)

    override fun status(new: Status): Response = ResponseWithContext(delegate.status(new), context)
}
