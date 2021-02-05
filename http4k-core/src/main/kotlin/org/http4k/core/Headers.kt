package org.http4k.core

internal fun Headers.headerValue(name: String) = find { it.first.equals(name, true) }?.second

internal fun Headers.headerValues(name: String) = filter { it.first.equals(name, true) }.map { it.second }

internal fun Headers.removeHeader(name: String) = filterNot { it.first.equals(name, true) }

internal fun Headers.removeHeaders(name: String) = filterNot { it.first.startsWith(name) }

internal fun Headers.replaceHeader(name: String, value: String?) = removeHeader(name).plus(name to value)

internal fun Headers.toHeaderMessage() = joinToString("\r\n") { "${it.first}: ${it.second}" }.plus("\r\n")

internal fun Headers.areSameHeadersAs(other: Headers) =
    all { header -> other.any { it == header } } &&
        other.all { otherHeader -> any { it == otherHeader } } &&
        withSameFieldNames()
            .all {
                other.withSameFieldNames()
                    .any { otherHeaders -> it == otherHeaders }
            }

private fun Headers.withSameFieldNames() =
    groupBy { (fieldName, _) -> fieldName }
        .filter { (_, headers) -> headers.size > 1 }
        .values
        .toList()

fun String?.safeLong(): Long? = try {
    this?.toLong()
} catch (e: Exception) {
    null
}
