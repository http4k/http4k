package org.http4k.connect.mcp.util

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.module.kotlin.KotlinModule
import org.http4k.connect.mcp.McpRpcMethod
import org.http4k.connect.mcp.ProtocolVersion
import org.http4k.format.ConfigurableJackson
import org.http4k.format.asConfigurable
import org.http4k.format.value
import org.http4k.format.withStandardMappings

object McpJson : ConfigurableJackson(
    KotlinModule.Builder().build()
        .asConfigurable()
        .withStandardMappings()
        .value(McpRpcMethod)
        .value(ProtocolVersion)
        .done()
        .setSerializationInclusion(JsonInclude.Include.NON_NULL)
)
