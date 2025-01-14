package org.http4k.mcp

import org.http4k.connect.mcp.Root

class Roots : Iterable<Root> {
    private var list = emptyList<Root>()

    fun update(req: Root.List.Response) {
        list = req.roots
    }

    override fun iterator() = list.iterator()
}
