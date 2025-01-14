package org.http4k.mcp

import org.http4k.connect.mcp.Resource
import org.http4k.util.ObservableList

class Resources(list: List<ResourceBinding>) : ObservableList<ResourceBinding>(list) {
    fun list(req: Resource.List.Request) = Resource.List.Response(
        items.map(ResourceBinding::toResource)
    )

    fun read(req: Resource.Read.Request) = items.find { it.uri == req.uri }
        ?.read()
        ?.let { Resource.Read.Response(it) }
        ?: error("no resource")

    fun subscribe(req: Resource.Subscribe.Request) {
        return TODO()
    }

    fun unsubscribe(req: Resource.Unsubscribe.Request) {
        return TODO()
    }
}
