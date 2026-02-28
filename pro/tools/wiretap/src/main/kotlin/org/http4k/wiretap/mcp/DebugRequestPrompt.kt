package org.http4k.wiretap.mcp

import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.server.capability.PromptCapability
import org.http4k.ai.model.Role
import org.http4k.routing.bind

fun DebugRequestPrompt(): PromptCapability {
    val transactionId = Prompt.Arg.required("transaction_id", "The transaction ID to debug")

    return Prompt(
        PromptName.of("debug_request"),
        "Debug a specific HTTP request/response by examining its full details",
        transactionId
    ) bind { req ->
        val id = transactionId(req)
        PromptResponse(
            listOf(
                Message(
                    Role.Assistant, Content.Text(
                        """I'll debug transaction $id. Let me get the full details.
                        |
                        |Steps:
                        |1. Call `get_transaction` with id=$id to get full request/response details
                        |2. Examine request headers, body, and URL parameters
                        |3. Examine response status, headers, and body
                        |4. Check for trace correlation via traceparent header
                        |5. If a trace ID is found, call `get_trace` to see the full span tree
                        |6. Identify potential issues (malformed requests, error responses, missing headers)""".trimMargin()
                    )
                )
            ),
            "Debug a specific HTTP transaction"
        )
    }
}
