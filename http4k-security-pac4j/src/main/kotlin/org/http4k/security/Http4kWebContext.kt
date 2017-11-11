package org.http4k.security

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.cookies
import org.http4k.core.queries
import org.pac4j.core.context.Cookie
import org.pac4j.core.context.WebContext
import org.pac4j.core.context.session.SessionStore

class Http4kWebContext private constructor(internal val request: Request, private var sessionStore: Http4kSessionStore) : WebContext {

    companion object {
        internal operator fun invoke(request: Request, ss: SessionStore<WebContext>) = Http4kWebContext(request, ss as Http4kSessionStore)
    }

    private var location: String? = null

    private var status = OK
    private val mutations = mutableListOf<(Response) -> Response>()

    fun response(): Response = mutations.fold(Response(status)) { memo, next -> next(memo) }

    override fun getFullRequestURL(): String = request.uri.toString()

    override fun setRequestAttribute(name: String, value: Any?) = throw UnsupportedOperationException()

    override fun addResponseCookie(cookie: Cookie) {
        mutations += { it.cookie(cookie.toHttp4k()) }
    }

    override fun setSessionStore(new: SessionStore<*>?) {
        throw UnsupportedOperationException()
    }

    override fun getRequestMethod(): String = request.method.name

    override fun getRemoteAddr(): String = throw UnsupportedOperationException()

    override fun getRequestCookies() = request.cookies().map { it.fromHttp4k() }

    override fun setResponseContentType(content: String) {
        mutations += { it.header("Content-type", content) }
    }

    override fun getRequestParameter(name: String) = request.query(name)

    override fun getScheme(): String = request.uri.scheme

    override fun getSessionStore(): SessionStore<*>? = sessionStore

    override fun getRequestParameters() = request.uri.queries().groupBy { it.first }
        .mapValues {
            it.value.map { it.second }.toTypedArray()
        }.toMap()

    override fun getRequestAttribute(name: String) = sessionStore.get(this, name)

    override fun getRequestHeader(name: String) = request.header(name)

    override fun getServerPort(): Int = request.uri.port ?: 80

    override fun getPath(): String = request.uri.path

    override fun writeResponseContent(content: String) {
        mutations += { it.body(content) }
    }

    override fun getServerName(): String = request.uri.host

    override fun isSecure(): Boolean = request.uri.scheme == "https"

    override fun setResponseStatus(code: Int) {
        status = Status(code, "")
    }

    override fun setResponseHeader(name: String, value: String?) {
        mutations += { it.header(name, value) }
    }

    fun redirect(): Response {
        setResponseStatus(Status.TEMPORARY_REDIRECT.code)
//        setResponseHeader("Location", location ?: throw Bug("no location set but attempted to redirect"))
        return response()
    }
}

private fun Cookie.toHttp4k(): org.http4k.core.cookie.Cookie = org.http4k.core.cookie.Cookie(
    name, value, maxAge.toLong(), null, domain, path, isSecure, isHttpOnly)

private fun org.http4k.core.cookie.Cookie.fromHttp4k(): Cookie = Cookie(name, value).apply {
    maxAge = this@fromHttp4k.maxAge?.toInt() ?: 0
    domain = this@fromHttp4k.domain
    path = this@fromHttp4k.path
    isSecure = this@fromHttp4k.secure
    isHttpOnly = this@fromHttp4k.httpOnly
}
