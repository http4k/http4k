/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.ai.mcp.McpError
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.mpp.MppPaymentCheck.Required
import org.http4k.ai.mcp.mpp.MppPayments
import org.http4k.ai.mcp.mpp.MppToolFilter
import org.http4k.ai.mcp.mpp.mppCredential
import org.http4k.ai.mcp.mpp.mppReceipt
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.protocol.withExtensions
import org.http4k.ai.mcp.server.capability.then
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.testing.testMcpClient
import org.http4k.ai.model.ToolName
import org.http4k.connect.RemoteFailure
import org.http4k.connect.mpp.MppVerifier
import org.http4k.connect.mpp.model.Challenge
import org.http4k.connect.mpp.model.ChallengeId
import org.http4k.connect.mpp.model.ChargeRequest
import org.http4k.connect.mpp.model.Credential
import org.http4k.connect.mpp.model.Currency
import org.http4k.connect.mpp.model.PaymentAmount
import org.http4k.connect.mpp.model.PaymentIntent
import org.http4k.connect.mpp.model.PaymentMethod
import org.http4k.connect.mpp.model.Realm
import org.http4k.connect.mpp.model.Receipt
import org.http4k.connect.mpp.model.ReceiptStatus
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Uri
import org.http4k.lens.MetaKey
import org.http4k.routing.bind
import org.http4k.routing.mcp
import java.time.Instant
import java.util.UUID

/**
 * This example demonstrates how to create an MCP server with MPP payment-protected tools.
 * Payment credentials are sent via _meta["org.paymentauth/credential"] on the tool call request.
 */
fun main() {
    val method = PaymentMethod.of("tempo")
    val intent = PaymentIntent.of("charge")

    val mcp = mcpServerWithPaidTool(method, intent)

    val credential = Credential(
        challenge = Challenge(
            id = ChallengeId.of(UUID.randomUUID().toString()),
            realm = Realm.of("api.example.com"),
            method = method,
            intent = intent,
            request = ChargeRequest(amount = PaymentAmount.of("1000"), currency = Currency.of("USD"))
        ),
        payload = mapOf("proof" to "0xsigned")
    )

    val credentialLens = MetaKey.mppCredential().toLens()

    mcp.testMcpClient(Request(POST, "/mcp")).use {
        it.start()
        println("--- calling without payment ---")
        printResult(it.tools().call(ToolName.of("premium_data"), ToolRequest()))

        println("\n--- calling with payment ---")
        printResult(
            it.tools().call(ToolName.of("premium_data"), ToolRequest(meta = Meta(credentialLens of credential)))
        )

        println("\n--- calling free tool ---")
        printResult(it.tools().call(ToolName.of("free_data"), ToolRequest()))
    }
}

private fun mcpServerWithPaidTool(method: PaymentMethod, intent: PaymentIntent) = mcp(
    ServerMetaData(McpEntity.of("http4k mpp mcp server"), Version.of("0.1.0"))
        .withExtensions(
            MppPayments(
                methods = listOf(method),
                intents = listOf(intent)
            )
        ),
    NoMcpSecurity,
    paidTool(method, intent),
    freeTool()
)

private fun paidTool(method: PaymentMethod, intent: PaymentIntent): org.http4k.ai.mcp.server.capability.ToolCapability {
    val challenge = Challenge(
        id = ChallengeId.of(UUID.randomUUID().toString()),
        realm = Realm.of("api.example.com"),
        method = method,
        intent = intent,
        request = ChargeRequest(amount = PaymentAmount.of("1000"), currency = Currency.of("USD"))
    )

    val verifier = MppVerifier { cred ->
        if (cred.payload["proof"] == "0xsigned") {
            Success(
                Receipt(
                    status = ReceiptStatus.success,
                    method = cred.challenge.method,
                    timestamp = Instant.now(),
                    challengeId = cred.challenge.id
                )
            )
        } else {
            Failure(RemoteFailure(POST, Uri.of("https://verify.example.com"), BAD_REQUEST, "bad signature"))
        }
    }

    val paymentFilter = MppToolFilter(verifier) { Required(listOf(challenge)) }

    return paymentFilter.then(
        Tool("premium_data", "get premium data (requires payment)") bind {
            Ok(listOf(Content.Text("Here is your premium data!")))
        }
    )
}

private fun freeTool() = Tool("free_data", "get free data") bind {
    Ok(listOf(Content.Text("Here is your free data!")))
}

private fun printResult(call: dev.forkhandles.result4k.Result<ToolResponse, McpError>) {
    when (call) {
        is Success -> when (val value = call.value) {
            is Ok -> {
                println("  result: ${value.content?.firstOrNull()}")
                val receipt = MetaKey.mppReceipt().toLens()(value.meta)
                if (receipt != null) println("  receipt: $receipt")
            }

            is ToolResponse.Error -> println("  payment required: ${value.content}")
            else -> println("  unexpected: $call")
        }

        is Failure -> println("  error: ${call.reason}")
    }
}
