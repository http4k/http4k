package org.http4k.wiretap.traffic

import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.TransactionFilter

data class TransactionFilterSignals(
    val direction: String? = null,
    val host: String? = null,
    val method: String? = null,
    val status: String? = null,
    val path: String? = null
) {
    constructor(filter: TransactionFilter) : this(
        direction = filter.direction?.name,
        host = filter.host,
        method = filter.method,
        status = filter.status,
        path = filter.path
    )

    fun toFilter() = TransactionFilter(
        direction = direction?.ifEmpty { null }?.let(Direction::valueOf),
        host = host?.ifEmpty { null },
        method = method?.ifEmpty { null },
        status = status?.ifEmpty { null },
        path = path?.ifEmpty { null }
    )
}
