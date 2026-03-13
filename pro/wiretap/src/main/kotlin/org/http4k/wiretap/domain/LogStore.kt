/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.domain

import io.opentelemetry.sdk.logs.data.LogRecordData
import java.util.concurrent.ConcurrentLinkedDeque

interface LogStore {
    fun record(log: LogRecordData)
    fun forTrace(traceId: String): List<LogRecordData>

    companion object {
        fun InMemory(maxLogs: Int = 5000) = object : LogStore {
            private val logs = ConcurrentLinkedDeque<LogRecordData>()

            override fun record(log: LogRecordData) {
                logs.addFirst(log)
                while (logs.size > maxLogs) {
                    logs.removeLast()
                }
            }

            override fun forTrace(traceId: String): List<LogRecordData> =
                logs.filter { it.spanContext.traceId == traceId }
        }
    }
}
