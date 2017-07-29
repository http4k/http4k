package org.http4k.hamkrest

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import org.http4k.core.Uri

fun hasUriPath(expected: String) = has("Path", { u: Uri -> u.path }, equalTo(expected))

fun hasUriQuery(expected: String) = has("Query", { u: Uri -> u.query }, equalTo(expected))

fun hasAuthority(expected: String) = has("Authority", { u: Uri -> u.authority }, equalTo(expected))

fun hasHost(expected: String) = has("Host", { u: Uri -> u.host }, equalTo(expected))

fun hasPort(expected: Int) = has("Port", { u: Uri -> u.port }, equalTo(expected))
