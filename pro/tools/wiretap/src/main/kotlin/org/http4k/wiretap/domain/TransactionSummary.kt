package org.http4k.wiretap.domain

data class TransactionSummary(
    val id: Long,
    val direction: Direction,
    val method: String,
    val uri: String,
    val path: String,
    val host: String,
    val status: Int,
    val durationMs: Long,
    val timestamp: String,
    val isChaos: Boolean,
    val isReplay: Boolean,
    val chaosInfo: String?
)

data class HeaderEntry(val name: String, val value: String)

data class TransactionDetail(
    val id: Long,
    val direction: String,
    val method: String,
    val uri: String,
    val status: Int,
    val durationMs: Long,
    val timestamp: String,
    val queryParams: List<HeaderEntry>,
    val requestHeaders: List<HeaderEntry>,
    val responseHeaders: List<HeaderEntry>,
    val requestBody: String,
    val responseBody: String,
    val curl: String,
    val traceId: String?,
    val isChaos: Boolean,
    val isReplay: Boolean
)
