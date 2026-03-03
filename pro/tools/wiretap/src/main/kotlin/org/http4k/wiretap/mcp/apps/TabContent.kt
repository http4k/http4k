package org.http4k.wiretap.mcp.apps

import org.http4k.ai.mcp.apps.model.AvailableMcpApp
import org.http4k.core.Method.GET
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel
import org.http4k.wiretap.mcp.McpAppsTabResetSignals
import org.http4k.wiretap.mcp.mcpTabResponse

data class TabContent(val apps: List<AvailableMcpApp>) : ViewModel

fun TabContent(mcpApps: List<AvailableMcpApp>, elements: DatastarElementRenderer) =
    "/apps" bind GET to {
        elements.mcpTabResponse(TabContent(mcpApps), McpAppsTabResetSignals())
    }
