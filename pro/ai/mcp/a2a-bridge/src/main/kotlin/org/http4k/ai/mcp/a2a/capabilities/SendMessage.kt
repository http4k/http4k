/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.a2a.capabilities

import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.recover
import org.http4k.ai.a2a.client.A2AClient
import org.http4k.ai.a2a.model.A2ARole.ROLE_USER
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.MessageId
import org.http4k.ai.a2a.model.MessageResponse
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.model.ResponseStream
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Tool
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.lens.with
import org.http4k.routing.bind
import java.util.Random

fun SendMessage(card: AgentCard, clientFor: (Request) -> A2AClient, random: Random) = Tool(
    "send_message",
    buildString {
        append("Send a message to the ")
        append(card.name)
        append(" agent. ")
        append(card.description)
        if (card.skills.isNotEmpty()) {
            append("\n\nAgent capabilities:")
            card.skills.forEach { skill ->
                append("\n- ")
                append(skill.name)
                append(": ")
                append(skill.description)
                skill.examples?.takeIf { it.isNotEmpty() }?.let { examples ->
                    append(" (e.g. ")
                    append(examples.joinToString("; "))
                    append(")")
                }
            }
        }
    },
    messageArg, contextIdArg,
    output = sendMessageOutput
) bind { req ->
    val message = Message(
        messageId = MessageId.random(random),
        role = ROLE_USER,
        parts = listOf(Part.Text(messageArg(req))),
        contextId = contextIdArg(req)?.let(ContextId::of)
    )

    clientFor(req.connectRequest ?: Request(GET, ""))
        .message(message)
        .map { Ok().with(sendMessageOutput of it.toResult()) }
        .recover { Error(listOf(Content.Text(it.toString()))) }
}

fun MessageResponse.toResult(): SendMessageResult = when (this) {
    is Message -> SendMessageResult(message = this)
    is Task -> SendMessageResult(task = this)
    is ResponseStream -> {
        val items = toList()
        SendMessageResult(
            message = items.filterIsInstance<Message>().lastOrNull(),
            task = items.filterIsInstance<Task>().lastOrNull()
        )
    }
}
