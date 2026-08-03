/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.mpp

import org.http4k.ai.mcp.protocol.McpExtension
import org.http4k.connect.mpp.model.PaymentIntent
import org.http4k.connect.mpp.model.PaymentMethod

data class MppPayments(val methods: List<PaymentMethod>, val intents: List<PaymentIntent>) : McpExtension {
    override val name = "payment"
    override val config = mapOf(
        "methods" to methods.map { it.value },
        "intents" to intents.map { it.value }
    )
}
