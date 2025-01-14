package org.http4k.mcp

import org.http4k.connect.mcp.Root
import org.http4k.util.ObservableList

class Roots : ObservableList<Root>(emptyList()) {
    fun update(req: Root.List.Response) {
        items = req.roots
    }
}
