package org.http4k.ai.llm.model

import org.http4k.ai.llm.tools.ToolRequest
import org.http4k.ai.model.RequestId
import org.http4k.ai.model.ToolName
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("type")
sealed class Message {

    @JsonSerializable
    @PolymorphicLabel("system")
    data class System(val text: String) : Message()

    @JsonSerializable
    @PolymorphicLabel("user")
    data class User(val contents: List<Content> = emptyList()) : Message() {
        constructor(message: String) : this(listOf(Content.Text(message)))
    }

    @JsonSerializable
    @PolymorphicLabel("assistant")
    data class Assistant(val contents: List<Content> = emptyList(), val toolRequests: List<ToolRequest> = emptyList()) :
        Message()

    @JsonSerializable
    @PolymorphicLabel("tool")
    data class ToolResult(val id: RequestId, val tool: ToolName, val text: String) : Message()

    @JsonSerializable
    @PolymorphicLabel("custom")
    data class Custom(val attributes: Map<String, Any>) : Message()
}
