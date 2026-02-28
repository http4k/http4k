package org.http4k.wiretap.domain

import org.http4k.core.HttpMessage
import org.http4k.core.queries
import org.http4k.core.toCurl
import org.http4k.lens.contentType
import org.http4k.wiretap.util.formatBody
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val format: DateTimeFormatter =
    DateTimeFormatter.ofPattern("HH:mm:ss.SSS").withZone(ZoneId.systemDefault())

internal fun WiretapTransaction.toSummary(): TransactionSummary {
    val uri = transaction.request.uri
    return TransactionSummary(
        id = id,
        direction = direction,
        method = transaction.request.method.name,
        uri = uri.toString(),
        path = if (uri.query.isBlank()) uri.path else "${uri.path}?${uri.query}",
        host = when {
            direction != Direction.Outbound -> ""
            uri.host.isEmpty() -> ""
            uri.port != null -> "${uri.host}:${uri.port}"
            else -> uri.host
        },
        status = transaction.response.status.code,
        durationMs = transaction.duration.toMillis(),
        timestamp = format.format(transaction.start),
        isChaos = transaction.response.headerValues("x-http4k-chaos").firstOrNull() != null,
        isReplay = transaction.request.header("x-http4k-wiretap") == "replay",
        chaosInfo = transaction.response.headerValues("x-http4k-chaos").firstOrNull()
    )
}

internal fun WiretapTransaction.toDetail(): TransactionDetail {
    val req = transaction.request
    val resp = transaction.response
    return TransactionDetail(
        id = id,
        direction = direction.name,
        method = req.method.name,
        uri = req.uri.toString(),
        status = resp.status.code,
        durationMs = transaction.duration.toMillis(),
        timestamp = format.format(transaction.start),
        queryParams = req.uri.queries().map { HeaderEntry(it.first, it.second ?: "") },
        requestHeaders = req.headers.map { HeaderEntry(it.first, it.second ?: "") },
        responseHeaders = resp.headers.map { HeaderEntry(it.first, it.second ?: "") },
        requestBody = req.prettifyBody(),
        responseBody = resp.prettifyBody(),
        curl = req.toCurl(),
        traceId = traceparent(),
        isChaos = resp.headerValues("x-http4k-chaos").firstOrNull() != null,
        isReplay = req.header("x-http4k-wiretap") == "replay"
    )
}

private fun HttpMessage.prettifyBody() =
    formatBody(bodyString(), contentType()?.value ?: "")

internal fun WiretapTransaction.traceparent(): String? =
    (transaction.request.header("traceparent") ?: transaction.response.header("traceparent"))
        ?.split("-")?.getOrNull(1)

internal fun TransactionSummary.matches(filter: TransactionFilter) =
    (filter.direction == null || direction == filter.direction) &&
        (filter.method == null || method == filter.method.name) &&
        (filter.status == null || status == filter.status.code) &&
        (filter.path == null || uri.lowercase().contains(filter.path.lowercase())) &&
        (filter.host == null || host.lowercase().contains(filter.host.lowercase()))
