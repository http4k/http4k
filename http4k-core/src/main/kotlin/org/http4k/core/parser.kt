package org.http4k.core

fun Request.Companion.parse(request: String): Request {
    val lines = lines(request)
    val (method, uri) = parseRequestLine(lines[0])
    val headers = parseHeaders(headerLines(lines))
    val body = parseBody(bodyLines(lines))
    return headers.fold(Request(method, uri).body(body),
        { memo, param -> memo.header(param.first, param.second) })
}

fun Response.Companion.parse(response: String): Response {
    val lines = lines(response)
    val status = parseStatus(lines[0])
    val headers = parseHeaders(headerLines(lines))
    val body = parseBody(bodyLines(lines))
    return headers.fold(Response(status).body(body),
        { memo, param -> memo.header(param.first, param.second) })
}

private fun lines(request: String): List<String> {
    if (request.isBlank()) throw IllegalArgumentException("Empty message")
    return request.split("\r\n")
}

private fun parseStatus(value: String): Status {
    val values = value.split(" ", limit = 3)
    if (values.size < 3) throw IllegalArgumentException("Invalid status line: $value")
    val code = values[1].toIntOrNull() ?: throw IllegalArgumentException("Invalid HTTP status: ${values[1]}")
    return Status.status(code, values[2])
}

private fun parseBody(bodyLines: List<String>): Body = Body(bodyLines.joinToString(""))

private fun bodyLines(lines: List<String>): List<String> = lines.subList(lines.indexOf("") + 1, lines.size)

private fun parseHeaders(headerLines: List<String>): Parameters = headerLines.map(::parseHeader)

private fun parseHeader(line: String): Pair<String, String?> = line.split(": ").let { it[0] to it[1] }

private fun headerLines(lines: List<String>) = lines.subList(1, lines.indexOf(""))

private fun parseRequestLine(line: String): Pair<Method, Uri> {
    val tokens = line.split(" ")
    if (tokens.size < 2) throw IllegalArgumentException("Invalid request line: $line")
    val method = try {
        Method.valueOf(tokens[0])
    } catch (e: Exception) {
        throw IllegalArgumentException("Invalid method: ${tokens[0]}")
    }
    return method to Uri.of(tokens[1])
}