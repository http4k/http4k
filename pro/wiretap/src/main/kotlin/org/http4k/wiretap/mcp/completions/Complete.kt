/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.mcp.completions

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.CompletionRequest
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.Reference
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.util.SignalModel
import org.http4k.wiretap.util.auto

data class CompleteSignals(
    val refType: String = "",
    val refName: String = "",
    val argumentName: String = "",
    val argumentValue: String = ""
) : SignalModel

data class CompletionResultView(
    val values: List<String>,
    val total: Int?,
    val hasMore: Boolean?
) : ViewModel

private val completeSignalsLens = Body.auto<CompleteSignals>()

fun Complete(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/complete" bind POST to { req ->
        val signals = completeSignalsLens(req)

        val ref = when (signals.refType) {
            "template" -> mcpClient.resources().listTemplates()
                .map { templates -> templates.firstOrNull { it.name.value == signals.refName }?.uriTemplate?.value ?: signals.refName }
                .valueOrNull()
                ?.let { Reference.ResourceTemplate(it) }
                ?: Reference.ResourceTemplate(signals.refName)
            else -> Reference.Prompt(signals.refName)
        }

        val view = mcpClient.completions()
            .complete(ref, CompletionRequest(signals.argumentName, signals.argumentValue))
            .valueOrNull()
            ?.let { CompletionResultView(it.values, it.total, it.hasMore) }
            ?: CompletionResultView(emptyList(), null, null)

        Response(OK).datastarElements(
            elements(view),
            selector = Selector.of("#completion-result")
        )
    }
