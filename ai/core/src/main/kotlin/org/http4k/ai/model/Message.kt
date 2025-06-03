package org.http4k.ai.model

import org.http4k.ai.tools.ToolRequest
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
    data class User(val name: String? = null, val contents: List<AiContent>) : Message()

    @JsonSerializable
    @PolymorphicLabel("ai")
    data class Ai(val text: String? = null, val toolRequests: List<ToolRequest> = emptyList()) : Message()

    @JsonSerializable
    @PolymorphicLabel("tool")
    data class ToolResponse(val id: RequestId, val tool: ToolName, val content: String) : Message()

    @JsonSerializable
    @PolymorphicLabel("custom")
    data class Custom(val attributes: Map<String, Any>) : Message()
}
