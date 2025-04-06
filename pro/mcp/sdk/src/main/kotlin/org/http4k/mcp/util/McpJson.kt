package org.http4k.mcp.util

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Result4k
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.get
import org.http4k.connect.model.Base64Blob
import org.http4k.connect.withAiMappings
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.with
import org.http4k.format.ConfigurableMoshi
import org.http4k.format.ListAdapter
import org.http4k.format.MapAdapter
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiNodeAdapter
import org.http4k.format.MoshiNull
import org.http4k.format.SetAdapter
import org.http4k.format.ThrowableAdapter
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.McpMessageId
import org.http4k.mcp.model.Priority
import org.http4k.mcp.model.PromptName
import org.http4k.mcp.model.ResourceName
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.util.McpJson.json
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
        .value(McpEntity)
        .value(McpRpcMethod)
        .value(McpMessageId)
        .value(Priority)
        .value(ProtocolVersion)
        .value(PromptName)
        .value(ResourceName)
        .value(SessionId)
        .value(Version)
        .done()
)

@KotshiJsonAdapterFactory
object McpJsonFactory : JsonAdapter.Factory by KotshiMcpJsonFactory

fun Result4k<McpNodeType, McpNodeType>.asHttp() =
    when (val response = this) {
        is Success<McpNodeType> -> response.get().asHttp(ACCEPTED)
        is Failure<McpNodeType> -> response.get().asHttp(BAD_REQUEST)
    }

private fun McpNodeType.asHttp(status: Status) = when (this) {
    is MoshiNull -> Response(status)
    else -> Response(status).with(Body.json().toLens() of this)
}
