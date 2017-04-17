package org.reekwest.http.core.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import org.reekwest.http.core.ContentType
import org.reekwest.http.core.HttpMessage
import org.reekwest.http.core.header
import org.reekwest.http.core.headerValues
import java.nio.ByteBuffer

object Header : LensSpec<HttpMessage, String>(
    StringLocator("header",
        { request, name -> request.headerValues(name) },
        { req, name, values -> values.fold(req, { m, next -> m.header(name, next) }) }),
    ByteBuffer::asString, String::asByteBuffer) {

    object Common {
        val CONTENT_TYPE = map(::ContentType).optional("Content-Type")
    }
}
//
//object Cookies {
//    private val allCookies = StringLensSpec<HttpMessage>("header",
//        { request, _ -> request.headerValues("Cookie") },
//        { req, _, values -> values.fold(req, { m, next -> m.header("Cookie", next) }) })
//        .map { it.toCookieList().map { it.name to it }.toMap() }
//
//    fun named(cookieName: String): LensSpec<HttpMessage, Cookie?> =
//        allCookies.map({ it[cookieName] })
//
