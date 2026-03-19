/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol

import org.http4k.ai.mcp.protocol.ClientProtocolCapability.ElicitationForm
import org.http4k.ai.mcp.protocol.ClientProtocolCapability.ElicitationUrl
import org.http4k.ai.mcp.protocol.ClientProtocolCapability.Experimental
import org.http4k.ai.mcp.protocol.ClientProtocolCapability.RootChanged
import org.http4k.ai.mcp.protocol.ClientProtocolCapability.Sampling
import org.http4k.ai.mcp.protocol.ClientProtocolCapability.SamplingContext
import org.http4k.ai.mcp.protocol.ClientProtocolCapability.SamplingTools
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
    val sampling: SamplingCapability?,
    val experimental: Map<String, Any>?,
    val elicitation: Elicitation?,
    val tasks: Tasks?,
    val extensions: Map<String, Any>? = null,
) {
    constructor(vararg capabilities: ClientProtocolCapability = ClientProtocolCapability.entries.toTypedArray()) : this(
        Roots(capabilities.contains(RootChanged)),
        buildSampling(capabilities.toList()),
        if (capabilities.contains(Experimental)) emptyMap<String, Any>() else null,
        buildElicitation(capabilities.toList()),
        buildTasks(capabilities.toList())
    )

    companion object {
        val All = ClientCapabilities(
            Roots(true),
            SamplingCapability(
                emptyMap<String, Any>(),
                emptyMap<String, Any>()
            ),
            emptyMap<String, Any>(),
            Elicitation(emptyMap<String, Any>(), emptyMap<String, Any>()), null
        )

        @JsonSerializable
        data class SamplingCapability(
            val tools: Map<String, Any>? = null,
            val context: Map<String, Any>? = null
        )

        @JsonSerializable
        data class Elicitation(
            val form: Map<String, Any>? = null,
            val url: Map<String, Any>? = null
        )

        @JsonSerializable
        data class Roots(val listChanged: Boolean? = false)

        @JsonSerializable
        data class Tasks(
            val list: Map<String, Any>? = null,
            val cancel: Map<String, Any>? = null,
            val requests: TaskRequests? = null
        )

        @JsonSerializable
        data class TaskRequests(
            val sampling: SamplingRequests? = null,
            val elicitation: ElicitationRequests? = null
        )

        @JsonSerializable
        data class SamplingRequests(
            val createMessage: Map<String, Any>? = null
        )

        @JsonSerializable
        data class ElicitationRequests(
            val create: Map<String, Any>?? = null
        )

        private fun buildSampling(capabilities: List<ClientProtocolCapability>): SamplingCapability? {
            val hasSampling = capabilities.contains(Sampling)
            val hasTools = capabilities.contains(SamplingTools)
            val hasContext = capabilities.contains(SamplingContext)

            if (!hasSampling && !hasTools && !hasContext) return null

            return SamplingCapability(
                tools = if (hasTools) emptyMap() else null,
                context = if (hasContext) emptyMap() else null
            )
        }

        private fun buildElicitation(capabilities: List<ClientProtocolCapability>): Elicitation? {
            val hasForm = capabilities.contains(ElicitationForm)
            val hasUrl = capabilities.contains(ElicitationUrl)

            if (!hasForm && !hasUrl) return null

            return Elicitation(
                form = if (hasForm) emptyMap() else null,
                url = if (hasUrl) emptyMap() else null
            )
        }

        private fun buildTasks(capabilities: List<ClientProtocolCapability>): Tasks? {
            val hasList = capabilities.contains(TaskList)
            val hasCancel = capabilities.contains(TaskCancel)
            val hasSamplingCreateMessage = capabilities.contains(TaskSamplingCreateMessage)
            val hasElicitationCreate = capabilities.contains(TaskElicitationCreate)

            if (!hasList && !hasCancel && !hasSamplingCreateMessage && !hasElicitationCreate) return null

            return Tasks(
                list = if (hasList) emptyMap() else null,
                cancel = if (hasCancel) emptyMap() else null,
                requests = TaskRequests(
                    sampling = if (hasSamplingCreateMessage) SamplingRequests(emptyMap()) else null,
                    elicitation = if (hasElicitationCreate) ElicitationRequests(emptyMap()) else null
                )
            )
        }
    }
}
