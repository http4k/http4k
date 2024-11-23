package org.http4k.lens

import org.http4k.core.ContentType
import org.http4k.core.Request
import org.http4k.urlDecoded
import org.http4k.urlEncoded

val Header.DATASTAR_REQUEST get() = Header.boolean().defaulted("DATASTAR_REQUEST", false)

val Header.DATASTAR_CONTENT_TYPE
    get() = Header.map(::ContentType, ContentType::toHeaderValue).optional("CONTENT_TYPE")

fun Request.isDatastar() = Header.DATASTAR_REQUEST(this)

val Header.DATASTAR_DATA
    get() = Query.map(String::urlDecoded, String::urlEncoded).optional("datastar")
