package org.http4k.mcp.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiNodeAdapter
import org.http4k.format.SetAdapter
import org.http4k.format.ThrowableAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import org.http4k.mcp.model.CostPriority
import org.http4k.mcp.model.IntelligencePriority
import org.http4k.mcp.model.MaxTokens
import org.http4k.mcp.model.MimeType
import org.http4k.mcp.model.ModelIdentifier
import org.http4k.mcp.model.SpeedPriority
import org.http4k.mcp.model.StopReason
import org.http4k.mcp.model.Temperature
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.MessageId
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.Version
import se.ansman.kotshi.KotshiJsonAdapterFactory

typealias McpNodeType = MoshiNode

object McpJson : ConfigurableMoshi(
    Moshi.Builder()
        .add(McpJsonFactory)
        .addLast(ThrowableAdapter)
        .addLast(ListAdapter)
        .addLast(SetAdapter)
        .addLast(MapAdapter)
        .addLast(MoshiNodeAdapter)
        .asConfigurable()
        .withStandardMappings()
        .value(CostPriority)
        .value(IntelligencePriority)
        .value(MaxTokens)
        .value(McpRpcMethod)
        .value(MessageId)
        .value(MimeType)
        .value(ModelIdentifier)
        .value(ProtocolVersion)
        .value(SessionId)
        .value(SpeedPriority)
        .value(StopReason)
        .value(Temperature)
        .value(Version)
        .done()
)

@KotshiJsonAdapterFactory
object McpJsonFactory : JsonAdapter.Factory by KotshiMcpJsonFactory
