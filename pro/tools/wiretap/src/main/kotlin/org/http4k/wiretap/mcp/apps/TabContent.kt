package org.http4k.wiretap.mcp.apps

import org.http4k.ai.mcp.apps.model.AvailableMcpApp
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.datastar.Selector
import org.http4k.lens.datastarElements
import org.http4k.routing.bind
import org.http4k.template.DatastarElementRenderer
import org.http4k.template.ViewModel

data class TabContent(val apps: List<AvailableMcpApp>) : ViewModel

fun AppsTabContent(mcpApps: List<AvailableMcpApp>, elements: DatastarElementRenderer) =
    "/tab/apps" bind GET to {
        Response(OK).datastarElements(
            elements(TabContent(mcpApps)),
            selector = Selector.of("#mcp-content")
        )
    }
