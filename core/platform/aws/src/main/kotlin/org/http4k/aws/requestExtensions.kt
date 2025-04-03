package org.http4k.aws

import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.urlEncoded


internal fun Request.encodeUri() =
    uri(uri.encodePathAndFragment())

private fun Uri.encodePathAndFragment() = if (fragment.isBlank())
    path(path.urlEncodedPath())
else
    path("${path}#${fragment}".urlEncodedPath()).fragment("")

private fun String.urlEncodedPath() =
    split("/").joinToString("/") { it.urlEncoded().replace("+", "%20").replace("*", "%2A").replace("%7E", "~") }

