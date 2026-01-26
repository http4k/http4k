package org.http4k.ai.mcp.protocol

import org.http4k.ai.mcp.protocol.ClientProtocolCapability.ElicitationForm
import org.http4k.ai.mcp.protocol.ClientProtocolCapability.ElicitationUrl
import org.http4k.ai.mcp.protocol.ClientProtocolCapability.Experimental
import org.http4k.ai.mcp.protocol.ClientProtocolCapability.RootChanged
import org.http4k.ai.mcp.protocol.ClientProtocolCapability.Sampling
import org.http4k.ai.mcp.protocol.ClientProtocolCapability.TaskCancel
import org.http4k.ai.mcp.protocol.ClientProtocolCapability.TaskElicitationCreate
import org.http4k.ai.mcp.protocol.ClientProtocolCapability.TaskList
import org.http4k.ai.mcp.protocol.ClientProtocolCapability.TaskSamplingCreateMessage
import se.ansman.kotshi.JsonSerializable

/**
 * Determines what features the client supports.
 */
@JsonSerializable
@ConsistentCopyVisibility
data class ClientCapabilities internal constructor(
    val roots: Roots?,
    val sampling: Unit?,
    val experimental: Unit?,
    val elicitation: Elicitation?,
    val tasks: Tasks?,
) {
    constructor(vararg capabilities: ClientProtocolCapability = ClientProtocolCapability.entries.toTypedArray()) : this(
        Roots(capabilities.contains(RootChanged)),
        if (capabilities.contains(Sampling)) Unit else null,
        if (capabilities.contains(Experimental)) Unit else null,
        buildElicitation(capabilities.toList()),
        buildTasks(capabilities.toList())
    )

    companion object {
        val All = ClientCapabilities(Roots(true), Unit, Unit, Elicitation(Unit, Unit), null)

        @JsonSerializable
        data class Elicitation(
            val form: Unit? = null,
            val url: Unit? = null
        )

        @JsonSerializable
        data class Roots(val listChanged: Boolean? = false)

        @JsonSerializable
        data class Tasks(
            val list: Unit? = null,
            val cancel: Unit? = null,
            val requests: TaskRequests? = null
        )

        @JsonSerializable
        data class TaskRequests(
            val sampling: SamplingRequests? = null,
            val elicitation: ElicitationRequests? = null
        )

        @JsonSerializable
        data class SamplingRequests(
            val createMessage: Unit? = null
        )

        @JsonSerializable
        data class ElicitationRequests(
            val create: Unit? = null
        )

        private fun buildElicitation(capabilities: List<ClientProtocolCapability>): Elicitation? {
            val hasForm = capabilities.contains(ElicitationForm)
            val hasUrl = capabilities.contains(ElicitationUrl)

            if (!hasForm && !hasUrl) return null

            return Elicitation(
                form = if (hasForm) Unit else null,
                url = if (hasUrl) Unit else null
            )
        }

        private fun buildTasks(capabilities: List<ClientProtocolCapability>): Tasks? {
            val hasList = capabilities.contains(TaskList)
            val hasCancel = capabilities.contains(TaskCancel)
            val hasSamplingCreateMessage = capabilities.contains(TaskSamplingCreateMessage)
            val hasElicitationCreate = capabilities.contains(TaskElicitationCreate)

            if (!hasList && !hasCancel && !hasSamplingCreateMessage && !hasElicitationCreate) return null

            return Tasks(
                list = if (hasList) Unit else null,
                cancel = if (hasCancel) Unit else null,
                requests = TaskRequests(
                    sampling = if (hasSamplingCreateMessage) SamplingRequests(Unit) else null,
                    elicitation = if (hasElicitationCreate) ElicitationRequests(Unit) else null
                )
            )
        }
    }
}
