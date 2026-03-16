/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.x402

import org.http4k.ai.mcp.model.MetaField
import org.http4k.ai.mcp.util.auto
import org.http4k.connect.x402.X402Moshi
import org.http4k.connect.x402.action.SettledResponse
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentRequirements
import org.http4k.lens.MetaKey

sealed interface PaymentCheck {
    data class Required(val requirements: List<PaymentRequirements>) : PaymentCheck
    data object Free : PaymentCheck
}

fun MetaKey.x402PaymentPayload() = auto(MetaField<PaymentPayload>("x402/payment"), X402Moshi)
fun MetaKey.x402Settled() = auto(MetaField<SettledResponse>("x402/payment-response"), X402Moshi)
