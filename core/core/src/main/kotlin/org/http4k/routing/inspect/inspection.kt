package org.http4k.routing.inspect

import org.http4k.core.Request
import org.http4k.routing.RouteMatcher
import org.http4k.routing.RoutingHandler
import org.http4k.routing.inspect.EscapeMode.Ansi
import org.http4k.routing.inspect.ForegroundColour.Cyan
import org.http4k.routing.inspect.ForegroundColour.Default
import org.http4k.routing.inspect.ForegroundColour.Green
import org.http4k.routing.inspect.ForegroundColour.Red
import org.http4k.routing.inspect.ForegroundColour.Yellow
import org.http4k.routing.inspect.Variation.Strikethrough

private val cyan = TextStyle(Cyan)
private val structural = TextStyle(Default)

/**
 * Renders the routes as a path tree, every line the same colour.
 */
fun RoutingHandler<*, *, *>.prettify(escape: EscapeMode = Ansi): String =
    render(escape) { cyan }

/**
 * Renders the routes as a path tree, colouring each leaf by how it matches the request:
 * green = matched, yellow = path matched but method/predicate did not, red+strikethrough = unmatched.
 */
fun RoutingHandler<*, *, *>.prettify(request: Request, escape: EscapeMode = Ansi): String =
    render(escape) { route ->
        when (route.match(request).priority) {
            0 -> TextStyle(Green)
            1 -> TextStyle(Yellow)
            else -> TextStyle(Red, variation = Strikethrough)
        }
    }

// ponytail: parses RouteMatcher.toString() ("template=<path> AND <predicate>"); format is locked by the approval tests
private fun RoutingHandler<*, *, *>.render(escape: EscapeMode, leafStyle: (RouteMatcher<*, *>) -> TextStyle): String {
    val root = Node()
    val fallback = mutableListOf<Pair<String, RouteMatcher<*, *>>>()

    routes.forEach { route ->
        val parts = route.toString().split(" AND ", limit = 2)
        if (parts.size == 2 && parts[0].startsWith("template=")) {
            val segments = parts[0].removePrefix("template=").split("/").filter { it.isNotEmpty() }
            root.at(segments).leaves += parts[1] to route
        } else {
            fallback += route.toString() to route
        }
    }

    val lines = root.leafLines(0, escape, leafStyle) +
        root.lines(0, escape, leafStyle) +
        fallback
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.first })
            .map { (label, route) -> label.styled(leafStyle(route), escape) }

    return lines.joinToString("\n")
}

private class Node {
    val children = sortedMapOf<String, Node>(String.CASE_INSENSITIVE_ORDER)
    val leaves = mutableListOf<Pair<String, RouteMatcher<*, *>>>()

    fun at(segments: List<String>): Node =
        if (segments.isEmpty()) {
            this
        } else {
            children.getOrPut(segments.first()) { Node() }.at(segments.drop(1))
        }

    fun leafLines(depth: Int, escape: EscapeMode, leafStyle: (RouteMatcher<*, *>) -> TextStyle): List<String> =
        leaves
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.first })
            .map { (label, route) -> indent(depth) + label.styled(leafStyle(route), escape) }

    fun lines(depth: Int, escape: EscapeMode, leafStyle: (RouteMatcher<*, *>) -> TextStyle): List<String> =
        children.flatMap { (segment, child) ->
            listOf(indent(depth) + segment.styled(structural, escape)) +
                child.leafLines(depth + 1, escape, leafStyle) +
                child.lines(depth + 1, escape, leafStyle)
        }
}

private fun indent(depth: Int) = "  ".repeat(depth)
