package org.http4k.wiretap.mcp

import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Message
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.PromptName
import org.http4k.ai.mcp.server.capability.PromptCapability
import org.http4k.ai.model.Role
import org.http4k.routing.bind

fun AnalyzeTrafficPrompt(): PromptCapability =
    Prompt(
        PromptName.of("analyze_traffic"),
        "Analyze captured HTTP traffic for error patterns, slow requests, and anomalies"
    ) bind {
        PromptResponse(
            listOf(
                Message(
                    Role.Assistant, Content.Text(
                        """I'll analyze the captured HTTP traffic. Let me start by gathering an overview and recent transactions.
                        |
                        |Steps:
                        |1. Call `get_stats` to get an overview of traffic patterns
                        |2. Call `list_transactions` to see recent requests
                        |3. Look for error patterns (4xx/5xx responses)
                        |4. Identify slow requests (high latency)
                        |5. Check if chaos engineering is active and its impact
                        |6. Summarize findings with actionable insights""".trimMargin()
                    )
                )
            ),
            "Analyze HTTP traffic captured by Wiretap"
        )
    }

