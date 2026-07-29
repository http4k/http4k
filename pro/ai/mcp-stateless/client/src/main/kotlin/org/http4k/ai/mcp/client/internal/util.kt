/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.internal

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.McpError
import org.http4k.ai.mcp.McpError.Internal
import org.http4k.ai.mcp.McpError.Protocol
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.protocol.messages.DomainError
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage
import se.ansman.kotshi.JsonSerializable

internal inline fun <reified T : Any> McpNodeType.asOrFailure() = with(McpJson) {
    val obj = this@asOrFailure as MoshiObject
    val error = obj["error"]
    when {
        error != null -> Failure(Protocol(
            when (error) {
                is MoshiObject -> ErrorMessageWithData(
                    error["code"]?.let { integer(it).toInt() } ?: -1,
                    error["message"]?.let { text(it) } ?: "Unknown error",
                    error["data"]
                )

                else -> ErrorMessageWithData(-1, error.toString())
            }
        ))

        else -> resultFrom { asA<T>(compact(obj["result"] ?: nullNode())) }
            .flatMapFailure { Failure(Internal(it)) }
    }
}

@JsonSerializable
data class ErrorMessageWithData(override val code: Int, override val message: String, val data: McpNodeType? = null) :
    ErrorMessage(code, message)

fun toToolResponseOrError(response: McpTool.Call.Response.Result): ToolResponse = when (response.isError) {
    true -> Error(response.content, response.structuredContent?.let(McpJson::convert), response._meta)
    else -> Ok(response.content, response.structuredContent?.let(McpJson::convert), response._meta)
}

fun toResourceErrorOrFailure(mcpError: McpError) = when (mcpError) {
    is Protocol if mcpError.error.code == DomainError.CODE ->
        Success(ResourceResponse.Error(mcpError.error.message))

    else -> Failure(mcpError)
}

fun toPromptErrorOrFailure(mcpError: McpError) =
    when (mcpError) {
        is Protocol if mcpError.error.code == DomainError.CODE -> Success(PromptResponse.Error(mcpError.error.message))
        else -> Failure(mcpError)
    }

fun toCompletionErrorOrFailure(mcpError: McpError) =
    when (mcpError) {
        is Protocol if mcpError.error.code == DomainError.CODE ->
            Success(CompletionResponse.Error(mcpError.error.message))

        else -> Failure(mcpError)
    }
