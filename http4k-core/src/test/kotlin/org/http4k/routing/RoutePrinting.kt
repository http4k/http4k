package org.http4k.routing

fun RouterDescription.prettify(depth: Int = 0): String = (" ".repeat(depth * 2)).let { indent ->
    val lineBreak = if(description == "or") "\n" else ""
    val indentValue = if(description == "or") indent else ""
    if (children.isEmpty()) {
        description.coloured(ForegroundColour.Cyan)
    } else {
        "$lineBreak$indentValue(${children.joinToString("$lineBreak$indentValue $description ") { it.prettify(depth + 1) }})"
    }
}
