/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.conformance.server.tools

import org.http4k.ai.mcp.stateless.ToolResponse.Ok
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.ai.mcp.stateless.model.fromHeader
import org.http4k.ai.mcp.stateless.model.string
import org.http4k.routing.stateless.bind

private val value = Tool.Arg.string().required("value", "mirrored into Mcp-Param-Value", fromHeader("Value"))

fun customHeaderTool() = Tool(
    "test_custom_header", "Echoes an argument which is mirrored into a custom HTTP header", value
) bind { Ok(value(it)) }
