package org.http4k.core

import org.http4k.asByteBuffer
import org.http4k.asString
import org.http4k.core.Body.Companion.EMPTY
import org.http4k.core.HttpMessage.Companion.HTTP_1_1
import org.http4k.length
import org.http4k.lens.WebForm
import org.http4k.routing.RoutedRequest
import java.io.Closeable
import java.io.InputStream
import java.nio.ByteBuffer

typealias Headers = Parameters

/**
 * If this Body is NOT being returned to the caller (via a Server implementation or otherwise), close() should be
 * called.
 */
interface Body : Closeable {
    val stream: InputStream
    val payload: ByteBuffer

    /**
     * Will be `null` for bodies where it's impossible to a priori determine - e.g. StreamBody
     */
    val length: Long?

    companion object {
        @JvmStatic
        @JvmName("create")
        operator fun invoke(body: String): Body = MemoryBody(body)

        @JvmStatic
        @JvmName("create")
        operator fun invoke(body: ByteBuffer): Body = when {
            body.hasArray() -> MemoryBody(body)
            else -> MemoryBody(ByteArray(body.remaining()).also { body.get(it) })
        }

        @JvmStatic
        @JvmName("create")
        operator fun invoke(body: InputStream, length: Long? = null): Body = StreamBody(body, length)

        @JvmField
        val EMPTY: Body = MemoryBody(ByteBuffer.allocate(0))
    }
}

/**
 * Represents a body that is backed by an in-memory ByteBuffer. Closing this has no effect.
 **/
data class MemoryBody(override val payload: ByteBuffer) : Body {
    constructor(payload: String) : this(payload.asByteBuffer())
    constructor(payload: ByteArray) : this(ByteBuffer.wrap(payload))

    override val length get() = payload.length().toLong()
    override fun close() {}
    override val stream get() = payload.array().inputStream(payload.position(), payload.length())
    override fun toString() = payload.asString()
}

/**
 * Represents a body that is backed by a (lazy) InputStream. Operating with StreamBody has a number of potential
 * gotchas:
 * 1. Attempts to consume the stream will pull all of the contents into memory, and should thus be avoided.
 * This includes calling `equals()` and `payload`
 * 2. If this Body is NOT being returned to the caller (via a Server implementation or otherwise), close() should be called.
 * 3. Depending on the source of the stream, this body may or may not contain a known length.
 */
class StreamBody(override val stream: InputStream, override val length: Long? = null) : Body {
    override val payload: ByteBuffer by lazy { stream.use { ByteBuffer.wrap(it.readBytes()) } }

    override fun close() {
        stream.close()
    }

    override fun toString() = "<<stream>>"

    override fun equals(other: Any?) =
        when {
            this === other -> true
            other !is Body? -> false
            else -> payload == other?.payload
        }

    override fun hashCode() = payload.hashCode()
}

/**
 * HttpMessages are designed to be immutable, so any mutation methods return a modified copy of the message.
 */
interface HttpMessage : Closeable {
    val headers: Headers
    val body: Body
    val version: String

    /**
     * Returns a formatted wire representation of this message.
     */
    fun toMessage(): String

    /**
     * Retrieves the first header value with this name.
     */
    fun header(name: String): String? = headers.headerValue(name)

    /**
     * (Copy &) Adds a header value with this name.
     */
    fun header(name: String, value: String?): HttpMessage

    /**
     * (Copy &) Add all passed headers.
     */
    fun headers(headers: Headers): HttpMessage

    /**
     * Replace all headers with ones passed.
     */
    fun replaceHeaders(source: Headers): HttpMessage

    /**
     * (Copy &) Adds a header value with this name, replacing any previously set values.
     */
    fun replaceHeader(name: String, value: String?): HttpMessage

    /**
     * (Copy &) remove headers with this name.
     */
    fun removeHeader(name: String): HttpMessage

    /**
     * (Copy &) remove headers with this prefix. Default removes all headers.
     */
    fun removeHeaders(prefix: String = ""): HttpMessage

    /**
     * (Copy &) sets the body content.
     */
    fun body(body: Body): HttpMessage

    /**
     * (Copy &) sets the body content.
     */
    fun body(body: String): HttpMessage

    /**
     * (Copy &) sets the body content.
     */
    fun body(body: InputStream, length: Long? = null): HttpMessage

    /**
     * Retrieves all header values with this name.
     */
    fun headerValues(name: String): List<String?> = headers.headerValues(name)

    /**
     * This will realise any underlying stream.
     */
    fun bodyString(): String = String(body.payload.array())

    companion object {
        const val HTTP_1_1 = "HTTP/1.1"
        const val HTTP_2 = "HTTP/2"
    }

    /**
     * Closes the request. For server generated messages, this is called by all backend/client implementations,
     * but when consuming external responses in streaming mode, this should be used.
     */
    override fun close() = body.close()
}

enum class Method {
    GET, POST, PUT, DELETE, OPTIONS, TRACE, PATCH, PURGE, HEAD;

    companion object
}

interface Request : HttpMessage {
    val method: Method
    val uri: Uri
    val source: RequestSource?

    /**
     * (Copy &) sets the method.
     */
    fun method(method: Method): Request

    /**
     * (Copy &) sets the Uri.
     */
    fun uri(uri: Uri): Request

    /**
     * (Copy &) Adds a query value with this name.
     */
    fun query(name: String, value: String?): Request

    /**
     * Retrieves the first query value with this name.
     */
    fun query(name: String): String?

    /**
     * Retrieves all query values with this name.
     */
    fun queries(name: String): List<String?>

    /**
     * (Copy &) remove queries with this name.
     */
    fun removeQuery(name: String): Request

    /**
     * (Copy &) remove queries with this prefix. Default removes all queries.
     */
    fun removeQueries(prefix: String = ""): Request

    /**
     * (Copy &) sets request source.
     */
    fun source(source: RequestSource): Request

    override fun header(name: String, value: String?): Request

    override fun headers(headers: Headers): Request

    override fun replaceHeader(name: String, value: String?): Request

    override fun replaceHeaders(source: Headers): Request

    override fun removeHeader(name: String): Request

    override fun removeHeaders(prefix: String): Request

    override fun body(body: Body): Request

    override fun body(body: String): Request

    override fun body(body: InputStream, length: Long?): Request

    override fun toMessage() =
        listOf("$method $uri $version", headers.toHeaderMessage(), bodyString()).joinToString("\r\n")

    companion object {
        @JvmStatic
        @JvmOverloads
        @JvmName("create")
        operator fun invoke(method: Method, uri: Uri, version: String = HTTP_1_1): Request =
            MemoryRequest(method, uri, listOf(), EMPTY, version)

        @JvmStatic
        @JvmOverloads
        @JvmName("create")
        operator fun invoke(method: Method, uri: String, version: String = HTTP_1_1): Request =
            Request(method, Uri.of(uri), version)

        operator fun invoke(method: Method, template: UriTemplate, version: String = HTTP_1_1): Request =
            RoutedRequest(Request(method, template.toString(), version), template)

        operator fun invoke(
            method: Method,
            template: UriTemplate,
            parameters: Map<String, String>,
            version: String = HTTP_1_1
        ): Request = RoutedRequest(Request(method, template.generate(parameters), version), template)
    }
}

@Suppress("EqualsOrHashCode")
data class MemoryRequest(
    override val method: Method,
    override val uri: Uri,
    override val headers: Headers = listOf(),
    override val body: Body = EMPTY,
    override val version: String = HTTP_1_1,
    override val source: RequestSource? = null
) : Request {
    override fun method(method: Method): Request = copy(method = method)

    override fun uri(uri: Uri) = copy(uri = uri)

    override fun query(name: String, value: String?) = copy(uri = uri.query(name, value))

    override fun query(name: String): String? = uri.queries().findSingle(name)

    override fun queries(name: String): List<String?> = uri.queries().findMultiple(name)

    override fun header(name: String, value: String?) = copy(headers = headers.plus(name to value))

    override fun replaceHeaders(source: Headers) = copy(headers = source)

    override fun headers(headers: Headers) = copy(headers = this.headers + headers)

    override fun replaceHeader(name: String, value: String?) = copy(headers = headers.replaceHeader(name, value))

    override fun source(source: RequestSource) = copy(source = source)

    override fun removeHeader(name: String) = copy(headers = headers.removeHeader(name))

    override fun removeHeaders(prefix: String) = copy(headers = headers.removeHeaders(prefix))

    override fun removeQuery(name: String) = copy(uri = uri.removeQuery(name))

    override fun removeQueries(prefix: String) = copy(uri = uri.removeQueries(prefix))

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

    override fun replaceHeaders(source: Headers): Response

    override fun removeHeader(name: String): Response

    override fun removeHeaders(prefix: String): Response

    override fun body(body: Body): Response

    override fun body(body: String): Response

    override fun body(body: InputStream, length: Long?): Response

    fun status(new: Status): Response

    override fun toMessage(): String =
        listOf("$version $status", headers.toHeaderMessage(), bodyString()).joinToString("\r\n")

    companion object {
        @JvmStatic
        @JvmOverloads
        @JvmName("create")
        operator fun invoke(status: Status, version: String = HTTP_1_1): Response =
            MemoryResponse(status, listOf(), EMPTY, version)
    }
}

@Suppress("EqualsOrHashCode")
data class MemoryResponse(
    override val status: Status,
    override val headers: Headers = listOf(),
    override val body: Body = EMPTY,
    override val version: String = HTTP_1_1
) : Response {
    override fun header(name: String, value: String?) = copy(headers = headers + (name to value))

    override fun headers(headers: Headers) = copy(headers = this.headers + headers)

    override fun replaceHeader(name: String, value: String?) = copy(headers = headers.replaceHeader(name, value))

    override fun replaceHeaders(source: Headers) = copy(headers = source)

    override fun removeHeader(name: String) = copy(headers = headers.removeHeader(name))

    override fun removeHeaders(prefix: String) = copy(headers = headers.removeHeaders(prefix))

    override fun body(body: Body) = copy(body = body)

    override fun body(body: String) = copy(body = Body(body))

    override fun status(new: Status) = copy(status = new)

    override fun body(body: InputStream, length: Long?) = copy(body = Body(body, length))

    override fun toString(): String = toMessage()

    override fun equals(other: Any?) = (other is Response
        && headers.areSameHeadersAs(other.headers)
        && status == other.status
        && body == other.body)
}

data class RequestSource(val address: String, val port: Int? = 0, val scheme: String? = null)

fun <T : HttpMessage> T.with(vararg modifiers: (T) -> T): T = modifiers.fold(this) { memo, next -> next(memo) }

fun WebForm.with(vararg modifiers: (WebForm) -> WebForm) = modifiers.fold(this) { memo, next -> next(memo) }
