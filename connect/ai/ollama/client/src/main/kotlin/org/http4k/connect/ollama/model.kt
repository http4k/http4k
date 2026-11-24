package org.http4k.connect.ollama

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.ai.model.Role
import org.http4k.connect.model.Base64Blob
import se.ansman.kotshi.JsonSerializable

class Template private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Template>(::Template)
}

sealed class ResponseFormat {
    data object json : ResponseFormat()
    data class Schema(val schema: Map<String, Any>) : ResponseFormat()
}

@JsonSerializable
data class Message(
    val role: Role,
    val content: String,
    val images: List<Base64Blob>? = null,
    val tool_calls: List<ToolCall>? = null,
    val tool_name: String? = null,
    val tool_call_id: String? = null
) {
    constructor(role: Role, content: String, images: List<Base64Blob>? = null) :
        this(role, content, images, null, null, null)

    fun copy(role: Role = this.role, content: String = this.content, images: List<Base64Blob>? = this.images) =
        Message(role, content, images, tool_calls, tool_name, tool_call_id)

    companion object {
        fun User(content: String, images: List<Base64Blob>? = null) = Message(Role.User, content, images)
        fun System(content: String, images: List<Base64Blob>? = null) = Message(Role.System, content, images)
        fun Assistant(content: String, images: List<Base64Blob>? = null) = Message(Role.Assistant, content, images)

        fun ToolCalls(tool_calls: List<ToolCall>) = Message(Role.Assistant, "", tool_calls = tool_calls)

        fun ToolCallResult(tool_name: String, content: String, tool_call_id: String? = null) =
            Message(Role.Tool, content, tool_name = tool_name, tool_call_id = tool_call_id)
    }
}

@JsonSerializable
data class Tool(val function: FunctionSpec, val type: String = "function")

@JsonSerializable
data class FunctionSpec(
    val name: String,
    val parameters: Map<String, Any>,
    val description: String? = null
)

@JsonSerializable
data class ToolCall(val function: FunctionCall, val id: String? = null)

@JsonSerializable
data class FunctionCall(val name: String, val arguments: Map<String, Any>, val index: Int? = null)
