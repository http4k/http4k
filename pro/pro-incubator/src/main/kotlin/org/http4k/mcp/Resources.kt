package org.http4k.mcp

import org.http4k.connect.mcp.Resource

class Resources(list: List<ResourceBinding>) {
    fun list(convert: Resource.List.Request): Resource.List.Response {
        return TODO()
    }

    fun read(convert: Resource.Read.Request): Resource.Read.Response {
        return TODO()
    }

    fun subscribe(convert: Resource.Subscribe.Request) {

    }

    fun unsubscribe(convert: Resource.Unsubscribe.Request) {

    }
}
