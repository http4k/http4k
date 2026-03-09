/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.wiretap.traffic

import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Tool
import org.http4k.core.Method.DELETE
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.MorphMode
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.TemplateRenderer
import org.http4k.wiretap.WiretapFunction
import org.http4k.wiretap.domain.TransactionStore

fun ClearTransaction(transactionStore: TransactionStore) = object : WiretapFunction {
    override fun http(elements: DatastarElementRenderer, html: TemplateRenderer) = "/" bind DELETE to {
        transactionStore.clear()
        Response(OK).datastarElements(
            emptyList<String>(),
            MorphMode.inner,
            Selector.of("#tx-list")
        )
    }

    override fun mcp() = Tool(
        "clear_transactions",
        "Clear all recorded HTTP transactions"
    ) bind {
        transactionStore.clear()
        Ok(listOf(Text("All transactions cleared")))
    }
}
