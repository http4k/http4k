/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
import dev.forkhandles.result4k.Result
import dev.forkhandles.result4k.Success
import org.http4k.ai.mcp.McpError
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.capability.then
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.testing.testMcpClient
import org.http4k.ai.mcp.x402.PaymentCheck
import org.http4k.ai.mcp.x402.X402ToolFilter
import org.http4k.ai.mcp.x402.x402PaymentPayload
import org.http4k.ai.mcp.x402.x402Settled
import org.http4k.ai.model.ToolName
import org.http4k.connect.x402.FakeX402Facilitator
import org.http4k.connect.x402.Http
import org.http4k.connect.x402.X402Facilitator
import org.http4k.connect.x402.X402Moshi
import org.http4k.connect.x402.model.AssetAddress
import org.http4k.connect.x402.model.PaymentAmount
import org.http4k.connect.x402.model.PaymentNetwork
import org.http4k.connect.x402.model.PaymentPayload
import org.http4k.connect.x402.model.PaymentRequired
import org.http4k.connect.x402.model.PaymentRequirements
import org.http4k.connect.x402.model.PaymentScheme
import org.http4k.connect.x402.model.WalletAddress
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.format.MoshiNode
import org.http4k.lens.MetaKey
import org.http4k.routing.bind
import org.http4k.routing.mcp

/**
 * This example demonstrates how to create an MCP server with x402 payment-protected tools.
 * Payment is sent via _meta["x402/payment"] on the tool call request per the x402 MCP transport spec.
 */
fun main() {
    val http = FakeX402Facilitator()

    val scheme = PaymentScheme.of("exact")
    val network = PaymentNetwork.of("base-sepolia")

    val mcp = mcpServerWithPaidTool(scheme, network, http)

    val payment = PaymentPayload(
        x402Version = 2,
        scheme = scheme,
        network = network,
        payload = mapOf("signature" to "0xsigned"),
        resource = "https://api.example.com/data",
        description = "Paid resource"
    )

    val paymentLens = MetaKey.x402PaymentPayload().toLens()

    mcp.testMcpClient(Request(POST, "/mcp")).use {

        printResult(it.tools().call(ToolName.of("premium_data"), ToolRequest()))

        printResult(
            it.tools().call(ToolName.of("premium_data"), ToolRequest(meta = Meta(paymentLens of payment)))
        )
    }
}

private fun mcpServerWithPaidTool(scheme: PaymentScheme, network: PaymentNetwork, http: HttpHandler): PolyHandler {
    val requirements = PaymentRequirements(
        scheme = scheme,
        network = network,
        asset = AssetAddress.of("0x036CbD53842c5426634e7929541eC2318f3dCF7e"),
        amount = PaymentAmount.of("100"),
        payTo = WalletAddress.of("0x1234567890abcdef1234567890abcdef12345678"),
        maxTimeoutSeconds = 60
    )

    val facilitator = X402Facilitator.Http(Uri.of("https://some-tx-facilitator"), http)

    val payForMcpAccessFilter = X402ToolFilter(facilitator) { PaymentCheck.Required(listOf(requirements)) }

    val rawTool = Tool("premium_data", "get premium data (requires payment)") bind {
        Ok(listOf(Content.Text("Here is your premium data!")))
    }

    return mcp(
        ServerMetaData(McpEntity.of("http4k x402 mcp server"), Version.of("0.1.0")),
        NoMcpSecurity,
        payForMcpAccessFilter.then(rawTool)
    )
}

private fun printResult(call: Result<ToolResponse, McpError>) {
    if (call is Success<*>) {
        when (call.value) {
            is Ok -> {
                println("result is: " + (call.value as Ok).content?.firstOrNull())
                println("settlement is: " + MetaKey.x402Settled().toLens()((call.value as Ok).meta))
            }
            is ToolResponse.Error -> println("payment required: " + X402Moshi.convert<MoshiNode, PaymentRequired>((call.value as ToolResponse.Error).structuredContent!!))
        }
    }
}
