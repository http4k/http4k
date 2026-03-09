/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.traffic

import org.http4k.core.Method
import org.http4k.core.Status
import org.http4k.wiretap.domain.Direction
import org.http4k.wiretap.domain.TransactionFilter
import org.http4k.wiretap.util.SignalModel

data class TransactionFilterSignals(
    val direction: String? = null,
    val host: String? = null,
    val method: String? = null,
    val status: String? = null,
    val path: String? = null
) : SignalModel {
    fun toFilter() = TransactionFilter(
        direction = direction?.ifEmpty { null }?.let(Direction::valueOf),
        host = host?.ifEmpty { null },
        method = method?.ifEmpty { null }?.let { Method.valueOf(it) },
        status = status?.ifEmpty { null }?.let { Status(it.toInt(), null) },
        path = path?.ifEmpty { null }
    )
}
