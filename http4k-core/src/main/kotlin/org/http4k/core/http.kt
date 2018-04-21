@file:Suppress("UNCHECKED_CAST")

package org.http4k.core

import org.http4k.asString
import org.http4k.core.Body.Companion.EMPTY
import org.http4k.core.HttpMessage.Companion.HTTP_1_1
import java.io.Closeable
import java.io.InputStream
import java.nio.ByteBuffer

typealias Headers = Parameters

interface Body : Closeable {
    val stream: InputStream
    val payload: ByteBuffer
    val length: Long

    companion object {
        operator fun invoke(body: String): Body = MemoryBody(body)
        operator fun invoke(body: ByteBuffer): Body = MemoryBody(body)
        operator fun invoke(body: InputStream, length: Long? = null): Body = StreamBody(body, length)

        val EMPTY: Body = MemoryBody("")
    }
}

data class MemoryBody(override val payload: ByteBuffer) : Body {
    constructor(payload: String) : this(ByteBuffer.wrap(payload.toByteArray()))

    override val length: Long by lazy { payload.array().size.toLong() }
    override fun close() {}
    override val stream: InputStream get() = payload.array().inputStream()
    override fun toString(): String = payload.asString()
}

class StreamBody(override val stream: InputStream, length: Long?) : Body {
    override val length: Long by lazy { length ?: throw IllegalStateException("Length is not set on StreamBody") }
    override val payload: ByteBuffer by lazy { stream.use { ByteBuffer.wrap(it.readBytes()) } }

    override fun close() {
        stream.close()
    }

    override fun toString(): String = "<<stream>>"

    override fun equals(other: Any?): Boolean =
            when {
                this === other -> true
                other !is Body? -> false
                else -> payload == other?.payload
            }

    override fun hashCode(): Int = payload.hashCode()
}

interface HttpMessage : Closeable {
    val headers: Headers
    val body: Body
    val version: String

    fun toMessage(): String

    fun header(name: String): String? = headers.headerValue(name)

    fun header(name: String, value: String?): HttpMessage

    fun headers(headers: Headers): HttpMessage

    fun replaceHeader(name: String, value: String?): HttpMessage

    fun removeHeader(name: String): HttpMessage

    fun body(body: Body): HttpMessage

    fun body(body: String): HttpMessage

    fun body(body: InputStream, length: Long? = null): HttpMessage

    fun headerValues(name: String): List<String?> = headers.headerValues(name)

    /**
     * This will realise any underlying stream
     */
    fun bodyString(): String = String(body.payload.array())

    companion object {
        const val HTTP_1_1 = "HTTP/1.1"
        const val HTTP_2 = "HTTP/2"
    }

    override fun close() = body.close()
}

enum class Method { GET, POST, PUT, DELETE, OPTIONS, TRACE, PATCH, PURGE, HEAD }

interface Request : HttpMessage {
    val method: Method
    val uri: Uri

    fun method(method: Method): Request

    fun uri(uri: Uri): Request

    fun query(name: String, value: String): Request

    fun query(name: String): String?

    fun queries(name: String): List<String?>

    override fun header(name: String, value: String?): Request

    override fun headers(headers: Headers): Request

    override fun replaceHeader(name: String, value: String?): Request

    override fun removeHeader(name: String): Request

    override fun body(body: Body): Request

    override fun body(body: String): Request

    override fun body(body: InputStream, length: Long?): Request

    override fun toMessage() = listOf("$method $uri $version", headers.toHeaderMessage(), bodyString()).joinToString("\r\n")

    companion object {
        operator fun invoke(method: Method, uri: Uri, version: String = HTTP_1_1): Request = MemoryRequest(method, uri, listOf(), EMPTY, version)
        operator fun invoke(method: Method, uri: String, version: String = HTTP_1_1): Request = MemoryRequest(method, Uri.of(uri), listOf(), EMPTY, version)
    }
}

@Suppress("EqualsOrHashCode")
data class MemoryRequest(override val method: Method, override val uri: Uri, override val headers: Headers = listOf(), override val body: Body = EMPTY, override val version: String = HTTP_1_1) : Request {
    override fun method(method: Method): Request = copy(method = method)

    override fun uri(uri: Uri) = copy(uri = uri)

    override fun query(name: String, value: String) = copy(uri = uri.query(name, value))

    override fun query(name: String): String? = uri.queries().findSingle(name)

    override fun queries(name: String): List<String?> = uri.queries().findMultiple(name)

    override fun header(name: String, value: String?) = copy(headers = headers.plus(name to value))

    override fun headers(headers: Headers) = copy(headers = this.headers.plus(headers))

    override fun replaceHeader(name: String, value: String?) = copy(headers = headers.replaceHeader(name, value))

    override fun removeHeader(name: String) = copy(headers = headers.removeHeader(name))

    override fun body(body: Body) = copy(body = body)

    override fun body(body: String) = copy(body = Body(body))

    override fun body(body: InputStream, length: Long?) = copy(body = Body(body, length))

    override fun toString(): String = toMessage()

    override fun equals(other: Any?) = (other is Request
            && headers.areSameHeadersAs(other.headers)
            && method == other.method
            && uri == other.uri
            && body == other.body)
}

@Suppress("EqualsOrHashCode")
interface Response : HttpMessage {
    val status: Status

    override fun header(name: String, value: String?): Response

    override fun headers(headers: Headers): Response

    override fun replaceHeader(name: String, value: String?): Response

    override fun removeHeader(name: String): Response

    override fun body(body: Body): Response

    override fun body(body: String): Response

    override fun body(body: InputStream, length: Long?): Response

    override fun toMessage(): String = listOf("$version $status", headers.toHeaderMessage(), bodyString()).joinToString("\r\n")

    companion object {
        operator fun invoke(status: Status, version: String = HTTP_1_1): Response = MemoryResponse(status, listOf(), EMPTY, version)
    }
}

@Suppress("EqualsOrHashCode")
data class MemoryResponse(override val status: Status, override val headers: Headers = listOf(), override val body: Body = EMPTY, override val version: String = HTTP_1_1) : Response {
    override fun header(name: String, value: String?) = copy(headers = headers.plus(name to value))

    override fun headers(headers: Headers) = copy(headers = this.headers.plus(headers))

    override fun replaceHeader(name: String, value: String?) = copy(headers = headers.replaceHeader(name, value))

    override fun removeHeader(name: String) = copy(headers = headers.removeHeader(name))

    override fun body(body: Body) = copy(body = body)

    override fun body(body: String) = copy(body = Body(body))

    override fun body(body: InputStream, length: Long?) = copy(body = Body(body, length))

    override fun toString(): String = toMessage()

    override fun equals(other: Any?) = (other is Response
            && headers.areSameHeadersAs(other.headers)
            && status == other.status
            && body == other.body)
}

fun <T> T.with(vararg modifiers: (T) -> T): T = modifiers.fold(this, { memo, next -> next(memo) })

fun String.toBody(): Body = Body(this)

