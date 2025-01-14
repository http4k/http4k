package org.http4k.mcp

import org.http4k.connect.mcp.Resource
import org.http4k.util.ObservableList

class ResourceTemplates(list: List<ResourceTemplateBinding>) : ObservableList<ResourceTemplateBinding>(list) {
    fun list(req: Resource.Template.List.Request) =
        Resource.Template.List.Response(items.map(ResourceTemplateBinding::toTemplate))
}
