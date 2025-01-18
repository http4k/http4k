package org.http4k.mcp

import org.http4k.connect.mcp.McpResource
import org.http4k.routing.RoutedResourceTemplate
import org.http4k.util.ObservableList

class McpResourceTemplates(list: List<RoutedResourceTemplate>) : ObservableList<RoutedResourceTemplate>(list) {
    fun list(req: McpResource.Template.List.Request) =
        McpResource.Template.List.Response(items.map(RoutedResourceTemplate::toTemplate))
}
