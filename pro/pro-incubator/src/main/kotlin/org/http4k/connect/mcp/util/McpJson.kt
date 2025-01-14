package org.http4k.connect.mcp.util

import com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.connect.mcp.McpRpcMethod
import org.http4k.connect.mcp.ProtocolVersion
import org.http4k.connect.mcp.Version
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings
import org.http4k.mcp.MimeType
import org.http4k.mcp.SessionId

object McpJson : ConfigurableJackson(
    KotlinModule.Builder().build()
        .asConfigurable()
        .withStandardMappings()
        .value(McpRpcMethod)
        .value(MimeType)
        .value(ProtocolVersion)
        .value(SessionId)
        .value(Version)
        .done()
        .setSerializationInclusion(NON_NULL)
)
