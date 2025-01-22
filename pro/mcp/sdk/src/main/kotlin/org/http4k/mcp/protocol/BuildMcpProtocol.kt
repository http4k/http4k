package org.http4k.mcp.protocol

import org.http4k.mcp.features.Completions
import org.http4k.mcp.features.Logger
import org.http4k.mcp.features.Prompts
import org.http4k.mcp.features.Resources
import org.http4k.mcp.features.Roots
import org.http4k.mcp.features.Sampling
import org.http4k.mcp.features.Tools

fun interface BuildMcpProtocol<RESP : Any> {
    operator fun invoke(
        metaData: ServerMetaData,
        prompts: Prompts,
        tools: Tools,
        resources: Resources,
        completions: Completions,
        sampling: Sampling,
        roots: Roots,
        logger: Logger,
    ): AbstractMcpProtocol<RESP>
}
