package org.http4k.connect.mcp.util

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.connect.mcp.CostPriority
import org.http4k.connect.mcp.IntelligencePriority
import org.http4k.connect.mcp.MaxTokens
import org.http4k.connect.mcp.McpRpcMethod
import org.http4k.connect.mcp.ProtocolVersion
import org.http4k.connect.mcp.SpeedPriority
import org.http4k.connect.mcp.Temperature
import org.http4k.connect.mcp.Version
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import org.http4k.mcp.MessageId
import org.http4k.mcp.MimeType
import org.http4k.mcp.SessionId

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
