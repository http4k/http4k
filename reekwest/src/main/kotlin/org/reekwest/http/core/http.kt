@file:Suppress("UNCHECKED_CAST")

package org.reekwest.http.core

sealed class HttpMessage {
    abstract val headers: Headers
    abstract val body: Body?
    abstract fun toMessage(): String

    fun header(name: String): String? = headers.find { it.first.equals(name, true) }?.second

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
        fun get(uri: String, headers: Headers = listOf(), body: Body? = null) = get(Uri.uri(uri), headers, body)
        fun get(uri: Uri, headers: Headers = listOf(), body: Body? = null) = Request(Method.GET, uri, headers, body)
        fun post(uri: String, headers: Headers = listOf(), body: Body? = null) = post(Uri.uri(uri), headers, body)
        fun post(uri: Uri, headers: Headers = listOf(), body: Body? = null) = Request(Method.POST, uri, headers, body)
        fun put(uri: String, headers: Headers = listOf(), body: Body? = null) = put(Uri.uri(uri), headers, body)
        fun put(uri: Uri, headers: Headers = listOf(), body: Body? = null) = Request(Method.PUT, uri, headers, body)
        fun delete(uri: String, headers: Headers = listOf(), body: Body? = null) = delete(Uri.uri(uri), headers, body)
        fun delete(uri: Uri, headers: Headers = listOf(), body: Body? = null) = Request(Method.DELETE, uri, headers, body)
        fun options(uri: String, headers: Headers = listOf(), body: Body? = null) = options(Uri.uri(uri), headers, body)
        fun options(uri: Uri, headers: Headers = listOf(), body: Body? = null) = Request(Method.OPTIONS, uri, headers, body)
        fun trace(uri: String, headers: Headers = listOf(), body: Body? = null) = trace(Uri.uri(uri), headers, body)
        fun trace(uri: Uri, headers: Headers = listOf(), body: Body? = null) = Request(Method.TRACE, uri, headers, body)
        fun patch(uri: String, headers: Headers = listOf(), body: Body? = null) = patch(Uri.uri(uri), headers, body)
        fun patch(uri: Uri, headers: Headers = listOf(), body: Body? = null) = Request(Method.PATCH, uri, headers, body)
    }

    fun query(name: String, value: String) = copy(uri = uri.query(name, value))

    fun query(name: String): String? = uri.queries().findSingle(name)

    fun queries(name: String): List<String?> = uri.queries().findMultiple(name)

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

    override fun toMessage(): String = listOf("$version $status", headers.toMessage(), bodyString()).joinToString("\r\n")

    override fun toString(): String = toMessage()
}

private fun Headers.toMessage() = map { "${it.first}: ${it.second}" }.joinToString("\r\n").plus("\r\n")

fun <T : HttpMessage> T.header(name: String, value: String?): T = copy(headers = headers.plus(name to value))

fun <T : HttpMessage> T.replaceHeader(name: String, value: String?): T = copy(headers = headers.remove(name).plus(name to value))

fun <T : HttpMessage> T.removeHeader(name: String): T = copy(headers = headers.remove(name))

fun <T : HttpMessage> T.body(body: Body?): T = copy(body = body)

fun <T : HttpMessage> T.body(body: String): T = copy(body = body.toBody())

fun <T : HttpMessage> T.bodyString(body: String): T = body(body)

fun <T : HttpMessage> T.copy(headers: Parameters = this.headers, body: Body? = this.body): T = when (this) {
    is Request -> this.copy(headers = headers, body = body) as T
    is Response -> this.copy(headers = headers, body = body) as T
    else -> throw IllegalStateException("Unknown class $this")
}

fun <T : HttpMessage> T.with(vararg modifiers: (T) -> T): T = modifiers.fold(this, { memo, next -> next(memo) })

private fun Headers.remove(name: String) = filterNot { it.first.equals(name, true) }