@file:Suppress("UNCHECKED_CAST")

package org.http4k.core

import org.http4k.core.HttpMessage.Companion.version
import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.OPTIONS
import org.http4k.core.Method.PATCH
import org.http4k.core.Method.POST
import org.http4k.core.Method.PUT
import org.http4k.core.Method.TRACE
import java.nio.ByteBuffer

typealias Headers = Parameters

typealias Body = ByteBuffer

interface HttpMessage {
    val headers: Headers
    val body: Body?

    fun toMessage(): String

    fun header(name: String): String? = headers.find { it.first.equals(name, true) }?.second

    fun header(name: String, value: String?): HttpMessage

    fun replaceHeader(name: String, value: String?): HttpMessage

    fun removeHeader(name: String): HttpMessage

    fun body(body: Body?): HttpMessage

    fun body(body: String): HttpMessage

    fun headerValues(name: String): List<String?> = headers.filter { it.first.equals(name, true) }.map { it.second }

    fun bodyString(): String = body?.let { String(it.array()) } ?: ""

    companion object {
        val version = "HTTP/1.1"
    }
}

enum class Method { GET, POST, PUT, DELETE, OPTIONS, TRACE, PATCH }

interface Request : HttpMessage {
    val method: Method
    val uri: Uri

    fun uri(uri: Uri): Request

    fun query(name: String, value: String): Request

    fun query(name: String): String?

    fun queries(name: String): List<String?>

    override fun header(name: String, value: String?): Request

    override fun replaceHeader(name: String, value: String?): Request

    override fun removeHeader(name: String): Request

    override fun body(body: Body?): Request

    override fun body(body: String): Request

    override fun toMessage() = listOf("$method $uri $version", headers.toMessage(), bodyString()).joinToString("\r\n")

    companion object {
        operator fun invoke(method: Method, uri: Uri): Request = MemoryRequest(method, uri, listOf(), null)
        fun get(uri: String, headers: Headers = listOf(), body: Body? = null) = get(Uri.of(uri), headers, body)
        fun get(uri: Uri, headers: Headers = listOf(), body: Body? = null): Request = MemoryRequest(GET, uri, headers, body)
        fun post(uri: String, headers: Headers = listOf(), body: Body? = null) = post(Uri.of(uri), headers, body)
        fun post(uri: Uri, headers: Headers = listOf(), body: Body? = null): Request = MemoryRequest(POST, uri, headers, body)
        fun put(uri: String, headers: Headers = listOf(), body: Body? = null) = put(Uri.of(uri), headers, body)
        fun put(uri: Uri, headers: Headers = listOf(), body: Body? = null): Request = MemoryRequest(PUT, uri, headers, body)
        fun delete(uri: String, headers: Headers = listOf(), body: Body? = null) = delete(Uri.of(uri), headers, body)
        fun delete(uri: Uri, headers: Headers = listOf(), body: Body? = null): Request = MemoryRequest(DELETE, uri, headers, body)
        fun options(uri: String, headers: Headers = listOf(), body: Body? = null) = options(Uri.of(uri), headers, body)
        fun options(uri: Uri, headers: Headers = listOf(), body: Body? = null): Request = MemoryRequest(OPTIONS, uri, headers, body)
        fun trace(uri: String, headers: Headers = listOf(), body: Body? = null) = trace(Uri.of(uri), headers, body)
        fun trace(uri: Uri, headers: Headers = listOf(), body: Body? = null): Request = MemoryRequest(TRACE, uri, headers, body)
        fun patch(uri: String, headers: Headers = listOf(), body: Body? = null) = patch(Uri.of(uri), headers, body)
        fun patch(uri: Uri, headers: Headers = listOf(), body: Body? = null): Request = MemoryRequest(PATCH, uri, headers, body)
    }
}

data class MemoryRequest(override val method: Method, override val uri: Uri, override val headers: Headers = listOf(), override val body: Body? = null) : Request {
    override fun uri(uri: Uri) = copy(uri = uri)

    override fun query(name: String, value: String) = copy(uri = uri.query(name, value))

    override fun query(name: String): String? = uri.queries().findSingle(name)

    override fun queries(name: String): List<String?> = uri.queries().findMultiple(name)

    override fun header(name: String, value: String?) = copy(headers = headers.plus(name to value))

    override fun replaceHeader(name: String, value: String?) = copy(headers = headers.remove(name).plus(name to value))

    override fun removeHeader(name: String) = copy(headers = headers.remove(name))

    override fun body(body: Body?) = copy(body = body)

    override fun body(body: String) = copy(body = body.toBody())

    override fun toString(): String = toMessage()

}

interface Response : HttpMessage {
    val status: Status

    override fun header(name: String, value: String?): Response

    override fun replaceHeader(name: String, value: String?): Response

    override fun removeHeader(name: String): Response

    override fun body(body: Body?): Response

    override fun body(body: String): Response

    override fun toMessage(): String = listOf("$version $status", headers.toMessage(), bodyString()).joinToString("\r\n")

    companion object {
        operator fun invoke(status: Status): Response = MemoryResponse(status, listOf(), null)
        fun ok(headers: Headers = listOf(), body: Body? = null): Response = MemoryResponse(Status.OK, headers, body)
        fun notFound(headers: Headers = listOf(), body: Body? = null): Response = MemoryResponse(Status.NOT_FOUND, headers, body)
        fun badRequest(headers: Headers = listOf(), body: Body? = null): Response = MemoryResponse(Status.BAD_REQUEST, headers, body)
        fun serverError(headers: Headers = listOf(), body: Body? = null): Response = MemoryResponse(Status.INTERNAL_SERVER_ERROR, headers, body)
        fun movedPermanently(headers: Headers = listOf(), body: Body? = null): Response = MemoryResponse(Status.MOVED_PERMANENTLY, headers, body)
        fun movedTemporarily(headers: Headers = listOf(), body: Body? = null) = found(headers, body)
        fun found(headers: Headers = listOf(), body: Body? = null): Response = MemoryResponse(Status.FOUND, headers, body)
    }
}

data class MemoryResponse(override val status: Status, override val headers: Headers = listOf(), override val body: Body? = null) : Response {
    override fun header(name: String, value: String?) = copy(headers = headers.plus(name to value))

    override fun replaceHeader(name: String, value: String?) = copy(headers = headers.remove(name).plus(name to value))

    override fun removeHeader(name: String) = copy(headers = headers.remove(name))

    override fun body(body: Body?) = copy(body = body)

    override fun body(body: String) = copy(body = body.toBody())

    override fun toString(): String = toMessage()
}

fun <T : HttpMessage> T.copy(headers: Parameters = this.headers, body: Body? = this.body): T = when (this) {
    is MemoryRequest -> this.copy(headers = headers, body = body) as T
    is MemoryResponse -> this.copy(headers = headers, body = body) as T
    else -> throw IllegalStateException("Unknown class $this")
}

fun <T : HttpMessage> T.with(vararg modifiers: (T) -> T): T = modifiers.fold(this, { memo, next -> next(memo) })

fun String.toBody(): Body = ByteBuffer.wrap(toByteArray())

private fun Headers.remove(name: String) = filterNot { it.first.equals(name, true) }

private fun Headers.toMessage() = map { "${it.first}: ${it.second}" }.joinToString("\r\n").plus("\r\n")
