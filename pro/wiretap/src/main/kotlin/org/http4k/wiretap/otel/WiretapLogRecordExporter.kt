/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.common.CompletableResultCode.ofSuccess
import io.opentelemetry.sdk.logs.data.LogRecordData
import io.opentelemetry.sdk.logs.export.LogRecordExporter
import org.http4k.wiretap.domain.LogStore

class WiretapLogRecordExporter(private val logStore: LogStore) : LogRecordExporter {
    override fun export(logs: Collection<LogRecordData>): CompletableResultCode {
        logs.forEach { logStore.record(it) }
        return ofSuccess()
    }

    override fun flush() = ofSuccess()

    override fun shutdown() = ofSuccess()
}
