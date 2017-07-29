package org.http4k.hamkrest

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import org.http4k.core.ContentType
import org.http4k.core.HttpMessage
import org.http4k.lens.Header

fun hasHeader(name: String, expected: String?) = has("Header", { m: HttpMessage -> m.header(name) }, equalTo(expected))

fun hasHeader(name: String, expected: List<String?>) = has("Header", { m: HttpMessage -> m.headerValues(name) }, equalTo(expected))

fun hasContentType(expected: ContentType) = has("Content-Type", { m: HttpMessage -> Header.Common.CONTENT_TYPE(m) }, equalTo(expected))

fun hasBody(expected: String) = has("Body", { m: HttpMessage -> m.bodyString() }, equalTo(expected))
