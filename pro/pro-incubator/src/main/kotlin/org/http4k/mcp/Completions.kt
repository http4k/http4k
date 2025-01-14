package org.http4k.mcp

import org.http4k.connect.mcp.Completion

class Completions {
    fun complete(req: Completion.Request) = Completion.Response(Completion(listOf()))
}
