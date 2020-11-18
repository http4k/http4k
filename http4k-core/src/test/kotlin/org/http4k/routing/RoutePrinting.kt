package org.http4k.routing

import org.http4k.routing.ForegroundColour.*
import org.http4k.routing.RouterMatch.*
import org.http4k.routing.Variation.Strikethrough

fun RouterDescription.prettify(depth: Int = 0): String = (" ".repeat(depth * 2)).let { indent ->
    val lineBreak = if(description == "or") "\n" else ""
    val indentValue = if(description == "or") indent else ""
    if (children.isEmpty()) {
        description.coloured(Cyan)
    } else {
        "$lineBreak$indentValue(${children.joinToString("$lineBreak$indentValue $description ") { it.prettify(depth + 1) }})"
    }
}

fun RouterMatch.prettify(depth: Int = 0): String = (" ".repeat(depth * 2)).let { indent ->
    val lineBreak = if(this.description.description == "or") "\n" else ""
    val indentValue = if(this.description.description == "or") indent else ""
    if (this.subMatches.isEmpty()) {
        this.description.description.styled(colour)
    } else {
        "$lineBreak$indentValue${"(".styled(colour)}${this.subMatches.joinToString("$lineBreak$indentValue ${this.description.description.styled(colour)} ") { it.prettify(depth + 1) }}${")".styled(colour)}"
    }
}

private val RouterMatch.colour: TextStyle
    get() = when(this){
        is MatchingHandler,is MatchedWithoutHandler -> TextStyle(Green)
        is MethodNotMatched, is Unmatched -> TextStyle(Red, variation = Strikethrough)
    }


