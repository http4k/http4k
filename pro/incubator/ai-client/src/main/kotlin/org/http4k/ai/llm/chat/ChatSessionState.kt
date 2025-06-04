package org.http4k.ai.llm.chat


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
    @PolymorphicLabel("processing")
    data class Processing(val message: String) : ChatSessionState()

    @JsonSerializable
    @PolymorphicLabel("awaitingApproval")
    data class AwaitingApproval(val pendingTools: List<ToolRequest>) : ChatSessionState()

    @JsonSerializable
    @PolymorphicLabel("toolInvocation")
    data class ToolInvocation(val toolRequest: ToolRequest, val remainingTools: List<ToolRequest>) : ChatSessionState()

    @JsonSerializable
    @PolymorphicLabel("responding")
    data class Responding(val response: ChatResponse) : ChatSessionState()

    @JsonSerializable
    @PolymorphicLabel("finished")
    data object Finished : ChatSessionState()
}
