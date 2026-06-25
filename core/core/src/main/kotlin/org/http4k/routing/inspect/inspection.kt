package org.http4k.routing.inspect

import org.http4k.core.Request
import org.http4k.routing.RoutingHandler
import org.http4k.routing.inspect.EscapeMode.Ansi
import org.http4k.routing.inspect.ForegroundColour.Cyan
import org.http4k.routing.inspect.ForegroundColour.Green
import org.http4k.routing.inspect.ForegroundColour.Red
import org.http4k.routing.inspect.ForegroundColour.Yellow
import org.http4k.routing.inspect.Variation.Strikethrough

/**
 * Renders the (flat) list of routes, each coloured the same.
 */
fun RoutingHandler<*, *, *>.prettify(escape: EscapeMode = Ansi): String =
    routes
        .map { it.toString() }
        .sortedWith(String.CASE_INSENSITIVE_ORDER)
        .joinToString("\n") { it.styled(TextStyle(Cyan), escape) }

/**
 * Renders the (flat) list of routes, colouring each by how it matches the request:
 * green = matched, yellow = path matched but method/predicate did not, red+strikethrough = unmatched.
 */
fun RoutingHandler<*, *, *>.prettify(request: Request, escape: EscapeMode = Ansi): String =
    routes
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.toString() })
        .joinToString("\n") { route ->
            val style = when (route.match(request).priority) {
                0 -> TextStyle(Green)
                1 -> TextStyle(Yellow)
                else -> TextStyle(Red, variation = Strikethrough)
            }
            route.toString().styled(style, escape)
        }
