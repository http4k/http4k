/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel

import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.LogStore
import org.http4k.wiretap.domain.TraceStore
import org.http4k.wiretap.domain.TransactionStore
import org.http4k.wiretap.livingdoc.LivingDocSection
import org.http4k.wiretap.otel.breakdown.TabContentRenderer
import org.http4k.wiretap.otel.breakdown.defaultTraceReportTabs
import java.time.Clock

fun OTel(
    traceStore: TraceStore,
    logStore: LogStore,
    transactionStore: TransactionStore,
    clock: Clock,
    livingDocSections: List<LivingDocSection>,
    traceReportTabs: List<TabContentRenderer>
) = object : WiretapFunction {
    private val functions = listOf(
        ListTraces(traceStore, clock),
        GetTrace(traceStore, logStore, clock),
        GetTraceDiagrams(traceStore, traceReportTabs),
        GetTraceMarkdown(traceStore, transactionStore, livingDocSections),
    )

    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) =
        "otel" bind routes(functions.map { it.http(elements, html) } + Index(html))

    override fun mcp() = CapabilityPack(functions.map { it.mcp() })
}
