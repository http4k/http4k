package org.http4k.connect.mcp.util

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.connect.mcp.McpRpcMethod
import org.http4k.connect.mcp.protocol.ProtocolVersion
import org.http4k.connect.mcp.protocol.Version
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import org.http4k.mcp.MessageId
import org.http4k.mcp.MimeType
import org.http4k.mcp.SessionId
import org.http4k.mcp.model.CostPriority
import org.http4k.mcp.model.IntelligencePriority
import org.http4k.mcp.model.MaxTokens
import org.http4k.mcp.model.SpeedPriority
import org.http4k.mcp.model.Temperature

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
        .value(ProtocolVersion)
        .value(SessionId)
        .value(SpeedPriority)
        .value(Temperature)
        .value(Version)
        .done()
        .setSerializationInclusion(NON_NULL)
)
