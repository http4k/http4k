package org.http4k.routing.inspect

import org.http4k.routing.RouterDescription
import org.http4k.routing.RouterMatch
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.MethodNotMatched
import org.http4k.routing.RouterMatch.Unmatched

private val regularDescriptionStyle = TextStyle(ForegroundColour.Cyan)

fun RouterDescription.prettify(depth: Int = 0, escapeMode: EscapeMode = EscapeMode.Ansi) = PrettyNode(this).prettify(depth, escapeMode)

fun PrettyNode.prettify(depth: Int = 0, escapeMode: EscapeMode = EscapeMode.Ansi) = when (description) {
    "or" -> orRendering(depth, escapeMode, regularDescriptionStyle)
    "and" -> andRenderer(depth, escapeMode, regularDescriptionStyle)
    else -> normalRenderer(escapeMode, regularDescriptionStyle)
}

private fun PrettyNode.orRendering(depth: Int, escapeMode: EscapeMode, style: TextStyle): String =
    (" ".repeat(depth * 2)).let { indent ->
        if (children.isEmpty()) {
            description.styled(style, escapeMode)
        } else {
            "\n$indent(${children.joinToString("\n$indent $description ") { it.prettify(depth + 1, escapeMode) }})"
        }
    }

private fun PrettyNode.andRenderer(depth: Int, escapeMode: EscapeMode, style: TextStyle): String =
    if (children.isEmpty()) {
        description.styled(style, escapeMode)
    } else {
        "(${children.joinToString(" $description ") { it.prettify(depth + 1, escapeMode) }})"
    }

private fun PrettyNode.normalRenderer(escapeMode: EscapeMode, style: TextStyle) =
    description.styled(style, escapeMode)

fun RouterMatch.prettify(depth: Int = 0, escapeMode: EscapeMode = EscapeMode.Ansi): String = (" ".repeat(depth * 2)).let { indent ->
    val lineBreak = if (description.description == "or") "\n" else ""
    val indentValue = if (description.description == "or") indent else ""
    if (subMatches.isEmpty()) {
        description.description.styled(colour, escapeMode)
    } else {
        "$lineBreak$indentValue${"(".styled(colour, escapeMode)}${subMatches.joinToString("$lineBreak$indentValue ${description.description.styled(colour, escapeMode)} ") { it.prettify(depth + 1, escapeMode) }}${")".styled(colour, escapeMode)}"
    }
}

private val RouterMatch.colour: TextStyle
    get() = when (this) {
        is MatchingHandler, is MatchedWithoutHandler -> TextStyle(ForegroundColour.Green)
        is MethodNotMatched, is Unmatched -> TextStyle(ForegroundColour.Red, variation = Variation.Strikethrough)
    }

data class PrettyNode(val description: String, val children: List<PrettyNode>) {
    constructor(description: RouterDescription) : this(description.description, description.children.map { PrettyNode(it) })
    constructor(match: RouterMatch) : this(match.description.description, match.subMatches.map { PrettyNode(it) })
}
