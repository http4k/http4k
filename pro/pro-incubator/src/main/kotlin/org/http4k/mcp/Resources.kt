package org.http4k.mcp

import org.http4k.connect.mcp.Resource
import org.http4k.connect.mcp.ServerResponse.Empty

class Resources(list: List<ResourceBinding>) {
    fun list(req: Resource.List.Request): Resource.List.Response {
        return TODO()
    }

    fun read(req: Resource.Read.Request): Resource.Read.Response {
        return TODO()
    }

    fun subscribe(req: Resource.Subscribe.Request): Empty {
        return TODO()
    }

    fun unsubscribe(req: Resource.Unsubscribe.Request): Empty {
        return TODO()
    }
}
