/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.mcp.resources

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse.Error
import org.http4k.ai.mcp.ResourceResponse.Ok
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.Resource.Content.Blob
import org.http4k.ai.mcp.model.Resource.Content.Text
import org.http4k.ai.mcp.model.Resource.Content.Unknown
import org.http4k.core.Body
import org.http4k.core.Method.POST
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.util.SignalModel
import org.http4k.wiretap.util.auto

data class ReadResourceSignals(val resourceUri: String = "") : SignalModel

data class ResourceContentView(val uri: String, val mimeType: String, val text: String)

data class ResourceResultView(val contents: List<ResourceContentView>) : ViewModel

private val readResourceSignalsLens = Body.auto<ReadResourceSignals>()

fun ReadResource(mcpClient: McpClient, elements: DatastarElementRenderer) =
    "/read" bind POST to { req ->
        val signals = readResourceSignalsLens(req)
        val uri = Uri.of(signals.resourceUri)

        val result = mcpClient.resources().read(ResourceRequest(uri))
        val view = result.map { response ->
            when (response) {
                is Ok -> ResourceResultView(response.list.map { content ->
                    ResourceContentView(
                        uri = content.uri.toString(),
                        mimeType = content.mimeType?.value ?: "",
                        text = when (content) {
                            is Text -> content.text
                            is Blob -> content.blob.value
                            is Unknown -> ""
                        }
                    )
                })
                is Error -> ResourceResultView(listOf(ResourceContentView("", "", response.message)))
            }
        }.valueOrNull() ?: ResourceResultView(listOf(ResourceContentView("", "", "Resource read failed")))

        Response(OK).datastarElements(
            elements(view),
            selector = Selector.of("#resource-result")
        )
    }
