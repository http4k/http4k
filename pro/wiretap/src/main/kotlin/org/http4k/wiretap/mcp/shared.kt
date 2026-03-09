/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.mcp

import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.util.SignalModel
import org.http4k.wiretap.util.datastarSignal

data class McpTabResetSignals(
    val selectedItem: String = "",
    val iframeVisible: Boolean = false
) : SignalModel

data class McpAppsTabResetSignals(
    val selectedServerId: String = "",
    val iframeVisible: Boolean = false
) : SignalModel

fun DatastarElementRenderer.mcpTabResponse(viewModel: ViewModel, resetSignals: SignalModel) =
    Response(OK)
        .datastarSignal(resetSignals)
        .datastarElements(
            this(viewModel),
            selector = Selector.of("#mcp-content")
        )

data class McpListItem(val name: String, val description: String)

data class McpListTabContent(
    val items: List<McpListItem>,
    val typeName: String,
    val placeholderText: String
) : ViewModel {
    val typeNameLower = typeName.lowercase()
    val typePath = typeName.lowercase()
}

fun DatastarElementRenderer.mcpDetailResponse(viewModel: ViewModel) =
    Response(OK).datastarElements(
        this(viewModel),
        selector = Selector.of("#detail-panel")
    )
