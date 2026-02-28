package org.http4k.wiretap.domain

data class TransactionFilter(
    val direction: Direction? = null,
    val host: String? = null,
    val method: String? = null,
    val status: String? = null,
    val path: String? = null
) {
    fun normalize() = TransactionFilter(
        direction = direction,
        host = host?.ifEmpty { null },
        method = method?.ifEmpty { null },
        status = status?.ifEmpty { null },
        path = path?.ifEmpty { null }
    )
}
