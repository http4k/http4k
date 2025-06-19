package org.http4k.ai.llm.chat


import org.http4k.ai.llm.model.Content
import org.http4k.ai.llm.tools.ToolRequest
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.Polymorphic
import se.ansman.kotshi.PolymorphicLabel

@JsonSerializable
@Polymorphic("type")
sealed class ChatSessionState {

    @JsonSerializable
    @PolymorphicLabel("waiting")
    data object WaitingForInput : ChatSessionState()

    @JsonSerializable
    @PolymorphicLabel("awaitingApproval")
    data class AwaitingApproval(val contents: List<Content>, val pendingTools: List<ToolRequest>) : ChatSessionState()

    @JsonSerializable
    @PolymorphicLabel("responding")
    data class Responding(val contents: List<Content>) : ChatSessionState()
}
