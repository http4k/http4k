@file:Suppress("UNCHECKED_CAST")

package org.http4k.core

import org.http4k.core.Body.Companion.EMPTY
import org.http4k.core.HttpMessage.Companion.version
import java.nio.ByteBuffer

typealias Headers = Parameters

data class Body(val payload: ByteBuffer) {
    constructor(payload: String) : this(ByteBuffer.wrap(payload.toByteArray()))

    override fun toString(): String = String(payload.array())

    companion object {
        val EMPTY = Body("")
    }
}

interface HttpMessage {
    val headers: Headers
    val body: Body

    fun toMessage(): String

    fun header(name: String): String? = headers.find { it.first.equals(name, true) }?.second

    fun header(name: String, value: String?): HttpMessage

    fun replaceHeader(name: String, value: String?): HttpMessage

    fun removeHeader(name: String): HttpMessage

    fun body(body: Body): HttpMessage

    fun body(body: String): HttpMessage

    fun headerValues(name: String): List<String?> = headers.filter { it.first.equals(name, true) }.map { it.second }

    fun bodyString(): String = body.toString()

    companion object {
        val version = "HTTP/1.1"
    }
}

enum class Method { GET, POST, PUT, DELETE, OPTIONS, TRACE, PATCH, PURGE, HEAD }

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

    override fun body(body: Body): Request

    override fun body(body: String): Request

    override fun toMessage() = listOf("$method $uri $version", headers.toMessage(), bodyString()).joinToString("\r\n")

    companion object {
        operator fun invoke(method: Method, uri: Uri): Request = MemoryRequest(method, uri, listOf(), EMPTY)
        operator fun invoke(method: Method, uri: String): Request = MemoryRequest(method, Uri.of(uri), listOf(), EMPTY)
    }
}

data class MemoryRequest(override val method: Method, override val uri: Uri, override val headers: Headers = listOf(), override val body: Body = EMPTY) : Request {
    override fun uri(uri: Uri) = copy(uri = uri)

    override fun query(name: String, value: String) = copy(uri = uri.query(name, value))

    override fun query(name: String): String? = uri.queries().findSingle(name)

    override fun queries(name: String): List<String?> = uri.queries().findMultiple(name)

    override fun header(name: String, value: String?) = copy(headers = headers.plus(name to value))

    override fun replaceHeader(name: String, value: String?) = copy(headers = headers.remove(name).plus(name to value))

    override fun removeHeader(name: String) = copy(headers = headers.remove(name))

    override fun body(body: Body) = copy(body = body)

    override fun body(body: String) = copy(body = Body(body))

    override fun toString(): String = toMessage()

}

interface Response : HttpMessage {
    val status: Status

    override fun header(name: String, value: String?): Response

    override fun replaceHeader(name: String, value: String?): Response

    override fun removeHeader(name: String): Response

    override fun body(body: Body): Response

    override fun body(body: String): Response

    override fun toMessage(): String = listOf("$version $status", headers.toMessage(), bodyString()).joinToString("\r\n")

    companion object {
        operator fun invoke(status: Status): Response = MemoryResponse(status, listOf(), EMPTY)
    }
}

data class MemoryResponse(override val status: Status, override val headers: Headers = listOf(), override val body: Body = EMPTY) : Response {
    override fun header(name: String, value: String?) = copy(headers = headers.plus(name to value))

    override fun replaceHeader(name: String, value: String?) = copy(headers = headers.remove(name).plus(name to value))

    override fun removeHeader(name: String) = copy(headers = headers.remove(name))

    override fun body(body: Body) = copy(body = body)

    override fun body(body: String) = copy(body = Body(body))

    override fun toString(): String = toMessage()
}

fun <T : HttpMessage> T.with(vararg modifiers: (T) -> T): T = modifiers.fold(this, { memo, next -> next(memo) })

fun String.toBody(): Body = Body(this)

private fun Headers.remove(name: String) = filterNot { it.first.equals(name, true) }

private fun Headers.toMessage() = map { "${it.first}: ${it.second}" }.joinToString("\r\n").plus("\r\n")
