/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.otel.breakdown

import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.domain.TraceDetail

data class Tab(val id: String, val name: String, val content: String)

fun interface TabContentRenderer {
    fun render(detail: TraceDetail, renderer: TemplateRenderer): Tab?
}

data class TraceBreakdownView(val tabs: List<Tab>) : ViewModel

fun TemplateRenderer.renderTraceBreakdownView(
    detail: TraceDetail,
    tabs: List<TabContentRenderer> = emptyList()
    ) = TraceBreakdownView(tabs.mapNotNull { it.render(detail, this) })
