package org.http4k.mcp.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.withAiMappings
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
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.ResourceName
import org.http4k.mcp.model.SpeedPriority
import org.http4k.mcp.protocol.McpRpcMethod
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
        .withAiMappings()
        .value(Base64Blob)
        .value(CostPriority)
        .value(IntelligencePriority)
        .value(McpEntity)
        .value(McpRpcMethod)
        .value(McpMessageId)
        .value(ProtocolVersion)
        .value(PromptName)
        .value(ResourceName)
        .value(SessionId)
        .value(SpeedPriority)
        .value(Version)
        .done()
)

@KotshiJsonAdapterFactory
object McpJsonFactory : JsonAdapter.Factory by KotshiMcpJsonFactory
