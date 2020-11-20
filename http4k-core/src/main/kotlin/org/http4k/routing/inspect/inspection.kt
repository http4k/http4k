package org.http4k.routing.inspect

import org.http4k.routing.RouterDescription
import org.http4k.routing.RouterMatch
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.MethodNotMatched
import org.http4k.routing.RouterMatch.Unmatched
import org.http4k.routing.inspect.EscapeMode.Ansi

fun RouterDescription.prettify(depth: Int = 0, escape: EscapeMode = Ansi) = PrettyNode(this).prettify(depth, escape)

fun RouterMatch.prettify(depth: Int = 0, escape: EscapeMode = Ansi) = PrettyNode(this).prettify(depth, escape)

private data class PrettyNode(val name: String, val textStyle: TextStyle, val groupStyle: TextStyle, val children: List<PrettyNode>) {
    constructor(description: RouterDescription) : this(
        description.description,
        TextStyle(ForegroundColour.Cyan),
        TextStyle(),
        description.children.map { PrettyNode(it) }
    )

    constructor(match: RouterMatch) : this(
        match.description.description,
        match.resolveStyle(),
        match.resolveStyle(),
        match.subMatches.map { PrettyNode(it) }
    )

    fun prettify(depth: Int = 0, escapeMode: EscapeMode = Ansi) = when (name) {
        "or" -> orRendering(depth, escapeMode, textStyle)
        "and" -> andRenderer(depth, escapeMode)
        else -> name.styled(textStyle, escapeMode)
    }

    private fun orRendering(depth: Int, escapeMode: EscapeMode, style: TextStyle): String =
        (" ".repeat(depth * 2)).let { indent ->
            if (children.isEmpty()) {
                name.styled(style, escapeMode)
            } else {
                "\n$indent${"(".styled(groupStyle, escapeMode)}${children.joinToString("\n$indent ${name.styled(groupStyle, escapeMode)} ") { it.prettify(depth + 1, escapeMode) }}${")".styled(groupStyle, escapeMode)}"
            }
        }

    private fun andRenderer(depth: Int, escapeMode: EscapeMode): String =
        if (children.isEmpty()) {
            name.styled(textStyle, escapeMode)
        } else {
            "${"(".styled(groupStyle, escapeMode)}${children.joinToString(" ${name.styled(groupStyle, escapeMode)} ") { it.prettify(depth + 1, escapeMode) }}${")".styled(groupStyle, escapeMode)}"
        }
}

private fun RouterMatch.resolveStyle(): TextStyle = when (this) {
    is MatchingHandler, is MatchedWithoutHandler -> TextStyle(ForegroundColour.Green)
    is MethodNotMatched, is Unmatched -> TextStyle(ForegroundColour.Red, variation = Variation.Strikethrough)
}
