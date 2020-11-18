package org.http4k.routing.inspect

import org.http4k.routing.RouterDescription
import org.http4k.routing.RouterMatch
import org.http4k.routing.RouterMatch.MatchedWithoutHandler
import org.http4k.routing.RouterMatch.MatchingHandler
import org.http4k.routing.RouterMatch.MethodNotMatched
import org.http4k.routing.RouterMatch.Unmatched

fun RouterDescription.prettify(depth: Int = 0, escapeMode: EscapeMode = EscapeMode.Ansi): String = (" ".repeat(depth * 2)).let { indent ->
    val lineBreak = if (description == "or") "\n" else ""
    val indentValue = if (description == "or") indent else ""
    if (children.isEmpty()) {
        description.styled(TextStyle(ForegroundColour.Cyan), escapeMode)
    } else {
        "$lineBreak$indentValue(${children.joinToString("$lineBreak$indentValue $description ") { it.prettify(depth + 1, escapeMode) }})"
    }
}

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
