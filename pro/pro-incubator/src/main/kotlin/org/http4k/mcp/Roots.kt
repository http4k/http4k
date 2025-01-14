package org.http4k.mcp

import org.http4k.connect.mcp.Root
import org.http4k.core.Uri

class Roots(list: List<Root>) {
    fun list(req: Root.List.Request): Root.List.Response {
        return Root.List.Response(
            listOf(
                Root(Uri.of("http://asd.com"), "asd"),
                Root(Uri.of("http://google.com"), "name2"),
            )
        )
    }
}
