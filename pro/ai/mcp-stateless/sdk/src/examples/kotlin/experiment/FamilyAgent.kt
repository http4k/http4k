/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package experiment

import org.http4k.ai.mcp.stateless.CompletionRequest
import org.http4k.ai.mcp.stateless.CompletionResponse
import org.http4k.ai.mcp.stateless.ToolResponse
import org.http4k.ai.mcp.stateless.model.Content.Text
import org.http4k.ai.mcp.stateless.model.Reference
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.ai.mcp.stateless.model.string
import org.http4k.ai.mcp.stateless.protocol.ServerMetaData
import org.http4k.ai.mcp.stateless.server.capability.CompletionCapability
import org.http4k.ai.mcp.stateless.server.capability.ToolCapability
import org.http4k.ai.mcp.stateless.server.security.NoMcpSecurity
import org.http4k.routing.stateless.bind
import org.http4k.routing.stateless.mcp
import org.http4k.server.JettyLoom
import org.http4k.server.asServer
import java.io.File

fun getFamilyMembers(): CompletionCapability = Reference.Prompt("Family Members") bind { req: CompletionRequest ->
    CompletionResponse.Ok(listOf("Alice", "Bob", "Charlie", "David"))
}

fun saveToMyDisk(): ToolCapability {
    val fileName = Tool.Arg.string().required("filename")
    val content = Tool.Arg.string().required("content")
    return Tool(
        "saveFile", "Save a file to my disk", fileName,
        content
    ) bind { req ->
        File(fileName(req)).writeText(content(req))
        ToolResponse.Ok(Text("File saved ${fileName(req)}"))
    }
}

val familyAgent = mcp(
    ServerMetaData("my family agent", "1.0.0"),
    NoMcpSecurity,
    getFamilyMembers(),
    saveToMyDisk()
).asServer(JettyLoom(7500)).start()
