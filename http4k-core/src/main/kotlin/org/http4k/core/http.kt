@file:Suppress("UNCHECKED_CAST")

package org.http4k.core

import java.nio.ByteBuffer

typealias Headers = Parameters

typealias Body = ByteBuffer

sealed class HttpMessage {
    abstract val headers: Headers
    abstract val body: Body?
    abstract fun toMessage(): String

    fun header(name: String): String? = headers.find { it.first.equals(name, true) }?.second

    abstract fun header(name: String, value: String?): HttpMessage

    abstract fun replaceHeader(name: String, value: String?): HttpMessage

    abstract fun removeHeader(name: String): HttpMessage

    abstract fun body(body: Body?): HttpMessage

    abstract fun body(body: String): HttpMessage

    fun headerValues(name: String): List<String?> = headers.filter { it.first.equals(name, true) }.map { it.second }

    fun bodyString(): String = body.string()

    private fun Body?.string(): String = this?.let { String(it.array()) } ?: ""

    companion object {
        val version = "HTTP/1.1"
    }
}

enum class Method { GET, POST, PUT, DELETE, OPTIONS, TRACE, PATCH }

data class Request(val method: Method, val uri: Uri, override val headers: Headers = listOf(), override val body: Body? = null) : HttpMessage() {
    companion object {
        fun get(uri: String, headers: Headers = listOf(), body: Body? = null): Request = get(Uri.uri(uri), headers, body)
        fun get(uri: Uri, headers: Headers = listOf(), body: Body? = null): Request = Request(Method.GET, uri, headers, body)
        fun post(uri: String, headers: Headers = listOf(), body: Body? = null): Request = post(Uri.uri(uri), headers, body)
        fun post(uri: Uri, headers: Headers = listOf(), body: Body? = null): Request = Request(Method.POST, uri, headers, body)
        fun put(uri: String, headers: Headers = listOf(), body: Body? = null): Request = put(Uri.uri(uri), headers, body)
        fun put(uri: Uri, headers: Headers = listOf(), body: Body? = null): Request = Request(Method.PUT, uri, headers, body)
        fun delete(uri: String, headers: Headers = listOf(), body: Body? = null): Request = delete(Uri.uri(uri), headers, body)
        fun delete(uri: Uri, headers: Headers = listOf(), body: Body? = null): Request = Request(Method.DELETE, uri, headers, body)
        fun options(uri: String, headers: Headers = listOf(), body: Body? = null): Request = options(Uri.uri(uri), headers, body)
        fun options(uri: Uri, headers: Headers = listOf(), body: Body? = null): Request = Request(Method.OPTIONS, uri, headers, body)
        fun trace(uri: String, headers: Headers = listOf(), body: Body? = null): Request = trace(Uri.uri(uri), headers, body)
        fun trace(uri: Uri, headers: Headers = listOf(), body: Body? = null): Request = Request(Method.TRACE, uri, headers, body)
        fun patch(uri: String, headers: Headers = listOf(), body: Body? = null): Request = patch(Uri.uri(uri), headers, body)
        fun patch(uri: Uri, headers: Headers = listOf(), body: Body? = null): Request = Request(Method.PATCH, uri, headers, body)
    }

    fun query(name: String, value: String) = copy(uri = uri.query(name, value))

    fun query(name: String): String? = uri.queries().findSingle(name)

    fun queries(name: String): List<String?> = uri.queries().findMultiple(name)

    override fun header(name: String, value: String?) = copy(headers = headers.plus(name to value))
    override fun replaceHeader(name: String, value: String?) = copy(headers = headers.remove(name).plus(name to value))

    override fun removeHeader(name: String) = copy(headers = headers.remove(name))

    override fun body(body: Body?) = copy(body = body)

    override fun body(body: String) = copy(body = body.toBody())

    override fun toMessage() = listOf("$method $uri $version", headers.toMessage(), bodyString()).joinToString("\r\n")

    override fun toString(): String = toMessage()
}

data class Response(val status: Status, override val headers: Headers = listOf(), override val body: Body? = null) : HttpMessage() {
    companion object {

        fun ok(headers: Headers = listOf(), body: Body? = null) = Response(Status.OK, headers, body)
        fun notFound(headers: Headers = listOf(), body: Body? = null) = Response(Status.NOT_FOUND, headers, body)
        fun badRequest(headers: Headers = listOf(), body: Body? = null) = Response(Status.BAD_REQUEST, headers, body)
        fun serverError(headers: Headers = listOf(), body: Body? = null) = Response(Status.INTERNAL_SERVER_ERROR, headers, body)
        fun movedPermanently(headers: Headers = listOf(), body: Body? = null) = Response(Status.MOVED_PERMANENTLY, headers, body)
        fun movedTemporarily(headers: Headers = listOf(), body: Body? = null) = found(headers, body)
        fun found(headers: Headers = listOf(), body: Body? = null) = Response(Status.FOUND, headers, body)
    }

    override fun header(name: String, value: String?) = copy(headers = headers.plus(name to value))

    override fun replaceHeader(name: String, value: String?) = copy(headers = headers.remove(name).plus(name to value))

    override fun removeHeader(name: String) = copy(headers = headers.remove(name))

    override fun body(body: Body?) = copy(body = body)

    override fun body(body: String) = copy(body = body.toBody())

    override fun toMessage(): String = listOf("$version $status", headers.toMessage(), bodyString()).joinToString("\r\n")

    override fun toString(): String = toMessage()
}

fun <T : HttpMessage> T.copy(headers: Parameters = this.headers, body: Body? = this.body): T = when (this) {
    is Request -> this.copy(headers = headers, body = body) as T
    is Response -> this.copy(headers = headers, body = body) as T
    else -> throw IllegalStateException("Unknown class $this")
}

fun <T : HttpMessage> T.with(vararg modifiers: (T) -> T): T = modifiers.fold(this, { memo, next -> next(memo) })

fun String.toBody(): Body = ByteBuffer.wrap(toByteArray())

private fun Headers.remove(name: String) = filterNot { it.first.equals(name, true) }

private fun Headers.toMessage() = map { "${it.first}: ${it.second}" }.joinToString("\r\n").plus("\r\n")
