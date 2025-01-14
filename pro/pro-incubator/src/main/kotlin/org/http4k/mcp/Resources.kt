package org.http4k.mcp

import org.http4k.connect.mcp.Resource
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Uri

class Resources(list: List<ResourceBinding>) {
    fun list(req: Resource.List.Request): Resource.List.Response {
        return Resource.List.Response(
            listOf(
                Resource(
                    Uri.of("http://asd.com"),
                    "asd",
                    "description",
                    MimeType.of(APPLICATION_JSON)
                ),
                Resource(
                    Uri.of("http://google.com"),
                    "google",
                    "description",
                    MimeType.of(APPLICATION_JSON)
                ),
            )
        )
    }

    fun read(req: Resource.Read.Request): Resource.Read.Response {
        return Resource.Read.Response(
            listOf(Resource.Content.Text("asd", req.uri, MimeType.of(APPLICATION_JSON)))
        )
    }

    fun subscribe(req: Resource.Subscribe.Request) {
        return TODO()
    }

    fun unsubscribe(req: Resource.Unsubscribe.Request) {
        return TODO()
    }
}
