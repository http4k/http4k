package org.http4k.ai.mcp.apps

import org.http4k.template.ViewModel

data class Index(val tools: List<ToolOption>) : ViewModel
