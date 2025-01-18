package org.http4k.mcp.util

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import org.http4k.mcp.model.CostPriority
import org.http4k.mcp.model.IntelligencePriority
import org.http4k.mcp.model.MaxTokens
import org.http4k.mcp.model.MimeType
import org.http4k.mcp.model.ModelName
import org.http4k.mcp.model.SpeedPriority
import org.http4k.mcp.model.StopReason
import org.http4k.mcp.model.Temperature
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.MessageId
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.SessionId

object McpJson : ConfigurableJackson(
    KotlinModule.Builder().build()
        .asConfigurable()
        .withStandardMappings()
        .value(CostPriority)
        .value(IntelligencePriority)
        .value(MaxTokens)
        .value(McpRpcMethod)
        .value(MessageId)
        .value(MimeType)
        .value(ModelName)
        .value(ProtocolVersion)
        .value(SessionId)
        .value(SpeedPriority)
        .value(StopReason)
        .value(Temperature)
        .value(Version)
        .done()
        .setSerializationInclusion(NON_NULL)
)
