package org.http4k.datastar

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.core.Uri

/**
 * Represents a HTML Fragment for Datastar SSE transport
 */
class Element private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Element>(::Element)
}

/**
 * A script that moves the browser to the location. Datastar posts with fetch, which follows a redirect
 * without moving the page, so this is how a handler answers with one.
 */
fun Element.Companion.goTo(location: Uri, id: String? = null) =
    of("""<script${id?.let { " id=\"$it\"" }.orEmpty()}>window.location.assign(${location.toString().asScriptString()})</script>""")

private fun String.asScriptString() = "\"" + replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
    .replace("</", "<\\/") + "\""
