/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.mpp

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.http4k.ai.mcp.ToolFilter
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.mpp.MppPaymentCheck.Free
import org.http4k.ai.mcp.mpp.MppPaymentCheck.Required
import org.http4k.connect.mpp.MppMoshi
import org.http4k.connect.mpp.MppVerifier
import org.http4k.connect.mpp.model.Challenge
import org.http4k.lens.MetaKey

private val credentialLens = MetaKey.mppCredential().toLens()
private val receiptLens = MetaKey.mppReceipt().toLens()

fun MppToolFilter(
    verifier: MppVerifier,
    check: (ToolRequest) -> MppPaymentCheck
) = ToolFilter { next ->
    { request ->
        when (val result = check(request)) {
            is Free -> next(request)
            is Required -> {
                fun paymentRequiredError(challenges: List<Challenge>, message: String) = Error(
                    content = listOf(Text(message)),
                    structuredContent = MppMoshi.asJsonObject(mapOf("challenges" to challenges))
                )

                credentialLens(request.meta)?.let { credential ->
                    verifier.verify(credential)
                        .map { receipt ->
                            when (val response = next(request)) {
                                is Ok -> response.copy(meta = receiptLens(receipt, response.meta))
                                else -> response
                            }
                        }
                        .recover { paymentRequiredError(result.challenges, it.message ?: "Verification failed") }
                } ?: paymentRequiredError(result.challenges, "Payment required")
            }
        }
    }
}
